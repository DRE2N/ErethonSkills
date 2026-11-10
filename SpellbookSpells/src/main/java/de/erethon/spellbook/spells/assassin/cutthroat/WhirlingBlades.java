package de.erethon.spellbook.spells.assassin.cutthroat;

import de.erethon.spellbook.Spellbook;
import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.combat.CombatSourceContext;
import de.erethon.spellbook.aoe.AoE;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.vfx.SkillSignatureVfx;

import de.erethon.spellbook.spells.vfx.TerrainRing;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WhirlingBlades extends CutthroatBaseSpell {

    // Spins around the caster, dealing damage to all nearby enemies.
    // If an enemy is affected by bleeding, they take bonus damage and spread the effect to nearby enemies.

    private final double radius = data.getDouble("radius", 3);
    private final double bonusDamageMultiplier = data.getDouble("bonusDamageMultiplier", 1.8);
    private final double playerWoundMultiplier = data.getDouble("playerWoundMultiplier", 1.25);
    private final int maxWoundSpread = data.getInt("maxWoundSpread", 3);
    private final double bleedingSpreadRadius = data.getDouble("bleedingSpreadRadius", 2);
    private final int bleedingMinDuration = data.getInt("bleedingMinDuration", 5) * 20;
    private final int bleedingMaxDuration = data.getInt("bleedingMaxDuration", 10) * 20;
    private final int whirlDuration = data.getInt("whirlDuration", 20);

    private final EffectData bleedEffectIdentifier = Spellbook.getEffectData("Bleeding");

    public WhirlingBlades(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = whirlDuration + 100; // Keep alive for windup + whirl duration + cleanup buffer
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
        Location center = caster.getLocation();
        TerrainRing boundary = new TerrainRing(center, radius,
                "terrain_band_cutthroat", 40, 0.38f);
        org.bukkit.Bukkit.getScheduler().runTaskLater(Spellbook.getInstance().getImplementer(),
                boundary::close, 12L);
        SkillSignatureVfx.accent(center, getClass(), "lead", (float) (radius * 2), 12, true);
        caster.getWorld().playSound(center, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.8f, 0.6f);
    }
    private void executeWhirlingBlades() {
        Location center = caster.getLocation();
        Set<LivingEntity> affectedTargets = new HashSet<>();
        int bleedingDuration = (int) Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_MAGICAL, bleedingMinDuration, bleedingMaxDuration, "bleedingDuration");

        createSpinningSwordAnimation();

        List<Entity> nearbyEntities = caster.getNearbyEntities(radius, radius, radius);
        Set<LivingEntity> woundedAtStart = new HashSet<>();
        for (Entity nearby : nearbyEntities) {
            if (nearby instanceof LivingEntity living && living.hasEffect(bleedEffectIdentifier)) {
                woundedAtStart.add(living);
            }
        }
        int spreadRemaining = maxWoundSpread;

        for (Entity entity : nearbyEntities) {
            if (entity instanceof LivingEntity t && !entity.equals(caster)
                    && Spellbook.canAttack(caster, t) && caster.hasLineOfSight(t)) {
                double damage = Spellbook.getVariedAttributeBasedDamage(data, caster, t, true, Attribute.ADVANTAGE_PHYSICAL);
                boolean isBleeding = woundedAtStart.contains(t);

                if (isBleeding) {
                    damage *= t instanceof org.bukkit.entity.Player ? playerWoundMultiplier : bonusDamageMultiplier;
                }

                double before = t.getHealth() + t.getAbsorptionAmount();
                double finalDamage = damage;
                CombatSourceContext.runSpell(this, () -> t.damage(finalDamage, caster, PDamageType.PHYSICAL));
                if (t.getHealth() + t.getAbsorptionAmount() < before) {
                    affectedTargets.add(t);
                    playSlashEffect(t.getLocation(), isBleeding);
                    if (isBleeding) {
                        t.removeEffect(bleedEffectIdentifier);
                        spreadRemaining -= spreadBleedingEffect(t, bleedingDuration, spreadRemaining);
                    }
                }
            }
        }

        createWhirlwindAoE(center);
        playWhirlVisualEffect(center);
        playWhirlSoundEffect(center);
        triggerTraits(affectedTargets);
    }

    private int spreadBleedingEffect(LivingEntity sourceTarget, int bleedingDuration, int limit) {
        int spread = 0;
        Collection<Entity> nearbyBleedingEntities = sourceTarget.getLocation().getNearbyEntities(bleedingSpreadRadius, bleedingSpreadRadius, bleedingSpreadRadius);

        for (Entity bleedingEntity : nearbyBleedingEntities) {
            if (spread >= limit) {
                break;
            }
            if (bleedingEntity instanceof LivingEntity bleedingTarget &&
                !bleedingTarget.equals(caster) &&
                !bleedingTarget.equals(sourceTarget) &&
                !bleedingTarget.hasEffect(bleedEffectIdentifier) &&
                caster.hasLineOfSight(bleedingTarget) &&
                Spellbook.canAttack(caster, bleedingTarget)) {

                bleedingTarget.addEffect(caster, bleedEffectIdentifier, bleedingDuration, 1);
                playBleedingSpreadEffect(bleedingTarget.getLocation());
                spread++;
            }
        }
        return spread;
    }

    private void playBleedingSpreadEffect(Location location) {
        SkillSignatureVfx.accent(location.clone().add(0, 1, 0), getClass(),
                "wake", 1.4f, 13, false);
    }

    private void playSlashEffect(Location location, boolean isBleeding) {
        SkillSignatureVfx.accent(location.clone().add(0, 1, 0), getClass(),
                isBleeding ? "main" : "wake", 2.1f, 15, false);
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
        SkillSignatureVfx.accent(center, getClass(), "main", (float) (radius * 2), 24, true);
    }
    private void playWhirlSoundEffect(Location location) {
        location.getWorld().playSound(location, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.5f, 0.8f);
        location.getWorld().playSound(location, Sound.ITEM_TRIDENT_RETURN, 1.0f, 1.5f);
        location.getWorld().playSound(location, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.4f, 1.8f);
    }

    private void createSpinningSwordAnimation() {
        SkillSignatureVfx.accent(caster.getLocation().add(0, 1, 0), getClass(),
                "wake", (float) (radius * 1.6), whirlDuration, false);
    }
    @Override
    protected void addSpellPlaceholders() {
        spellAddedPlaceholders.add(Component.text(Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_MAGICAL, bleedingMinDuration, bleedingMaxDuration, "bleedingDuration") / 20, VALUE_COLOR));
        placeholderNames.add("bleedingDuration");
    }
}

