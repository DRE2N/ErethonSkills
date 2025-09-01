package de.erethon.spellbook.spells.assassin.cutthroat;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.aoe.AoE;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.assassin.AssassinBaseSpell;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.CircleEffect;
import de.slikey.effectlib.effect.SphereEffect;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WhirlingBlades extends AssassinBaseSpell {

    private final double radius = data.getDouble("radius", 3);
    private final double bonusDamageMultiplier = data.getDouble("bonusDamageMultiplier", 1.5);
    private final double bleedingSpreadRadius = data.getDouble("bleedingSpreadRadius", 2);
    private final int bleedingMinDuration = data.getInt("bleedingMinDuration", 5);
    private final int bleedingMaxDuration = data.getInt("bleedingMaxDuration", 10);
    private final int whirlDuration = data.getInt("whirlDuration", 20);

    private final EffectData bleedEffectIdentifier = Spellbook.getEffectData("Bleeding");

    public WhirlingBlades(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    public boolean onCast() {
        if (!super.onCast()) {
            return false;
        }

        playWhirlWindup();

        new BukkitRunnable() {
            @Override
            public void run() {
                executeWhirlingBlades();
            }
        }.runTaskLater(Spellbook.getInstance().getImplementer(), 10L);

        return true;
    }

    private void playWhirlWindup() {
        Location center = caster.getLocation().add(0, 1, 0);

        caster.getWorld().spawnParticle(Particle.SMOKE, center, 20, 0.5, 0.5, 0.5, 0.1);
        caster.getWorld().spawnParticle(Particle.ENCHANTED_HIT, center, 15, 0.8, 0.8, 0.8, 0.1);

        caster.getWorld().playSound(center, Sound.ITEM_TRIDENT_RETURN, 0.6f, 1.8f);
        caster.getWorld().playSound(center, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.8f, 0.6f);

        EffectManager effectManager = Spellbook.getInstance().getEffectManager();
        if (effectManager != null) {
            CircleEffect chargeRing = new CircleEffect(effectManager);
            chargeRing.setLocation(center);
            chargeRing.radius = (float) radius;
            chargeRing.particle = Particle.DUST;
            chargeRing.particles = 25;
            chargeRing.duration = 10;
            chargeRing.start();
        }
    }

    private void executeWhirlingBlades() {
        Location center = caster.getLocation();
        Set<LivingEntity> affectedTargets = new HashSet<>();
        int bleedingDuration = (int) Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_MAGICAL, bleedingMinDuration, bleedingMaxDuration, "bleedingDuration");

        List<Entity> nearbyEntities = caster.getNearbyEntities(radius, radius, radius);

        for (Entity entity : nearbyEntities) {
            if (entity instanceof LivingEntity t && !entity.equals(caster) && Spellbook.canAttack(caster, t)) {
                double damage = Spellbook.getVariedAttributeBasedDamage(data, caster, t, true, Attribute.ADVANTAGE_PHYSICAL);
                boolean isBleeding = t.hasEffect(bleedEffectIdentifier);

                if (isBleeding) {
                    damage *= bonusDamageMultiplier;
                    spreadBleedingEffect(t, bleedingDuration);
                    playBleedingSpreadEffect(t.getLocation());
                }

                t.damage(damage, caster);
                affectedTargets.add(t);

                playSlashEffect(t.getLocation(), isBleeding);
            }
        }

        createWhirlwindAoE(center);
        playWhirlVisualEffect(center);
        playWhirlSoundEffect(center);
        triggerTraits(affectedTargets);
    }

    private void spreadBleedingEffect(LivingEntity sourceTarget, int bleedingDuration) {
        Collection<Entity> nearbyBleedingEntities = sourceTarget.getLocation().getNearbyEntities(bleedingSpreadRadius, bleedingSpreadRadius, bleedingSpreadRadius);

        for (Entity bleedingEntity : nearbyBleedingEntities) {
            if (bleedingEntity instanceof LivingEntity bleedingTarget &&
                !bleedingTarget.equals(caster) &&
                !bleedingTarget.equals(sourceTarget) &&
                Spellbook.canAttack(caster, bleedingTarget)) {

                bleedingTarget.addEffect(caster, bleedEffectIdentifier, bleedingDuration, 1);
                playBleedingSpreadEffect(bleedingTarget.getLocation());
            }
        }
    }

    private void playBleedingSpreadEffect(Location location) {
        Location adjustedLoc = location.add(0, 1, 0);
        adjustedLoc.getWorld().spawnParticle(Particle.DUST, adjustedLoc, 8, 0.3, 0.3, 0.3, 0,
            new Particle.DustOptions(org.bukkit.Color.RED, 1.2f));
        adjustedLoc.getWorld().spawnParticle(Particle.DRIPPING_WATER, adjustedLoc, 3, 0.2, 0.2, 0.2);
        adjustedLoc.getWorld().playSound(adjustedLoc, Sound.BLOCK_HONEY_BLOCK_STEP, 0.3f, 0.8f);
    }

    private void playSlashEffect(Location location, boolean isBleeding) {
        Location adjustedLoc = location.add(0, 1, 0);

        adjustedLoc.getWorld().spawnParticle(Particle.SWEEP_ATTACK, adjustedLoc, 3, 0.3, 0.3, 0.3, 0.1);
        adjustedLoc.getWorld().spawnParticle(Particle.CRIT, adjustedLoc, 5, 0.2, 0.2, 0.2, 0.2);

        if (isBleeding) {
            adjustedLoc.getWorld().spawnParticle(Particle.BLOCK, adjustedLoc, 8, 0.4, 0.4, 0.4, 0.1,
                Material.REDSTONE_BLOCK.createBlockData());
            adjustedLoc.getWorld().playSound(adjustedLoc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.7f, 1.3f);
        } else {
            adjustedLoc.getWorld().playSound(adjustedLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.5f, 1.2f);
        }
    }

    private void createWhirlwindAoE(Location center) {
        createCircularAoE(center, radius, 1.0, whirlDuration)
                .onTick(aoe -> {
                    if (currentTicks % 5 == 0) {
                        Location aoeCenter = aoe.getCenter().add(0, 0.5, 0);
                        aoeCenter.getWorld().spawnParticle(Particle.DUST, aoeCenter, 5, radius * 0.8, 0.2, radius * 0.8, 0,
                            new Particle.DustOptions(org.bukkit.Color.SILVER, 1.0f));
                    }
                });
    }

    private void playWhirlVisualEffect(Location center) {
        Location effectCenter = center.add(0, 1, 0);

        EffectManager effectManager = Spellbook.getInstance().getEffectManager();
        if (effectManager != null) {
            SphereEffect whirlSphere = new SphereEffect(effectManager);
            whirlSphere.setLocation(effectCenter);
            whirlSphere.radius = (float) radius;
            whirlSphere.particles = 60;
            whirlSphere.particle = Particle.SWEEP_ATTACK;
            whirlSphere.duration = 15;
            whirlSphere.start();

            CircleEffect bladeRing = new CircleEffect(effectManager);
            bladeRing.setLocation(effectCenter);
            bladeRing.radius = (float) radius * 0.8f;
            bladeRing.particles = 40;
            bladeRing.particle = Particle.DUST;
            bladeRing.duration = 20;
            bladeRing.start();
        }

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= 20) {
                    this.cancel();
                    return;
                }

                double angle = ticks * 0.5;
                for (int i = 0; i < 4; i++) {
                    double bladeAngle = angle + (Math.PI * 2 * i / 4);
                    double x = Math.cos(bladeAngle) * radius * 0.9;
                    double z = Math.sin(bladeAngle) * radius * 0.9;
                    Location bladeLoc = effectCenter.clone().add(x, 0, z);

                    bladeLoc.getWorld().spawnParticle(Particle.DUST, bladeLoc, 2, 0.1, 0.1, 0.1, 0,
                        new Particle.DustOptions(org.bukkit.Color.WHITE, 1.2f));
                }

                ticks++;
            }
        }.runTaskTimer(Spellbook.getInstance().getImplementer(), 0L, 1L);
    }

    private void playWhirlSoundEffect(Location location) {
        location.getWorld().playSound(location, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.5f, 0.8f);
        location.getWorld().playSound(location, Sound.ITEM_TRIDENT_RETURN, 1.0f, 1.5f);
        location.getWorld().playSound(location, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.4f, 1.8f);
    }

    @Override
    protected void addSpellPlaceholders() {
        spellAddedPlaceholders.add(Component.text(Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_MAGICAL, bleedingMinDuration, bleedingMaxDuration, "bleedingDuration"), VALUE_COLOR));
        placeholderNames.add("bleedingDuration");
    }
}

