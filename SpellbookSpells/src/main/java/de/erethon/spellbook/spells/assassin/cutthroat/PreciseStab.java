package de.erethon.spellbook.spells.assassin.cutthroat;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.aoe.AoE;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellEffect;
import de.erethon.spellbook.spells.assassin.AssassinBaseSpell;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.LineEffect;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class PreciseStab extends AssassinBaseSpell {

    // Perform a quick, piercing stab on the target. Deals moderate physical damage.
    // If the target has 3 or more Bleed stacks, this attack consumes the bleeding to grant you Fury.
    // When Fury is gained, creates a blood wave cone that damages nearby enemies.

    private final int range = data.getInt("range", 3);
    private final int bleedStacksRequired = data.getInt("bleedStacksRequired", 3);
    private final int furyMinDuration = data.getInt("furyMinDuration", 6) * 20;
    private final int furyMaxDuration = data.getInt("furyMaxDuration", 12) * 20;
    private final double furyWaveRange = data.getDouble("furyWaveRange", 6.0);
    private final double furyWaveAngle = data.getDouble("furyWaveAngle", 60.0);

    private final EffectData bleedingEffectData = Spellbook.getEffectData("Bleeding");
    private final EffectData furyEffectData = Spellbook.getEffectData("Fury");

    public PreciseStab(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onPrecast() {
        return super.onPrecast() && lookForTarget(range);
    }

    @Override
    public boolean onCast() {
        playPreciseStabBuildup();

        new BukkitRunnable() {
            @Override
            public void run() {
                executeStab();
            }
        }.runTaskLater(Spellbook.getInstance().getImplementer(), 8L); // 0.4 second delay

        return super.onCast();
    }

    private void playPreciseStabBuildup() {
        Location targetLoc = target.getEyeLocation();
        Vector direction = targetLoc.subtract(caster.getEyeLocation()).toVector().normalize();

        EffectManager effectManager = Spellbook.getInstance().getEffectManager();
        if (effectManager != null) {
            LineEffect targetingLine = new LineEffect(effectManager);
            targetingLine.setLocation(caster.getEyeLocation());
            targetingLine.setTarget(target.getEyeLocation());
            targetingLine.particle = Particle.CRIT;
            targetingLine.particles = 15;
            targetingLine.duration = 8;
            targetingLine.start();
        }

        caster.getWorld().playSound(caster.getLocation(), Sound.ITEM_CROSSBOW_LOADING_START, 0.7f, 1.8f);
        target.getWorld().spawnParticle(Particle.ENCHANTED_HIT, target.getEyeLocation(), 8, 0.2, 0.2, 0.2, 0.1);
    }

    private void executeStab() {
        int bleedingOnTarget = 0;
        SpellEffect bleedingEffect = null;
        for (SpellEffect effect : target.getEffects()) {
            if (effect.data == bleedingEffectData) {
                bleedingOnTarget += effect.getStacks();
                bleedingEffect = effect;
            }
        }

        playStrikeEffect();

        double physicalDamage = Spellbook.getVariedAttributeBasedDamage(data, caster, target, true, Attribute.ADVANTAGE_PHYSICAL);
        target.damage(physicalDamage, caster, PDamageType.PHYSICAL);

        if (bleedingOnTarget >= bleedStacksRequired && bleedingEffect != null) {
            int remainingStacks = bleedingOnTarget - bleedStacksRequired;
            if (remainingStacks <= 0) {
                target.removeEffect(bleedingEffectData);
            } else {
                bleedingEffect.setStacks(remainingStacks);
            }

            int furyDuration = (int) Spellbook.getRangedValue(data, caster, target, Attribute.ADVANTAGE_MAGICAL, furyMinDuration, furyMaxDuration, "furyDuration");
            caster.addEffect(caster, furyEffectData, furyDuration, 1);

            createBloodWave();

            caster.getWorld().playSound(target.getLocation(), Sound.BLOCK_CONDUIT_ATTACK_TARGET, 0.8f, 1.2f);
            caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_WITHER_HURT, 0.4f, 1.8f);
            target.getWorld().spawnParticle(Particle.BLOCK, target.getLocation(), 20, 0.5, 0.5, 0.5, 0.1, Material.REDSTONE_BLOCK.createBlockData());
        } else {
            caster.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.6f, 1.0f);
        }
    }

    private void playStrikeEffect() {
        Location strikeLocation = target.getLocation().add(0, 1, 0);
        target.getWorld().spawnParticle(Particle.CRIT, strikeLocation, 15, 0.3, 0.3, 0.3, 0.2);
        target.getWorld().spawnParticle(Particle.ENCHANTED_HIT, strikeLocation, 8, 0.2, 0.2, 0.2, 0.1);

        caster.getWorld().playSound(strikeLocation, Sound.ITEM_CROSSBOW_SHOOT, 1.0f, 1.5f);
    }

    private void createBloodWave() {
        Vector direction = target.getLocation().subtract(caster.getLocation()).toVector().normalize();
        Location waveOrigin = caster.getLocation().add(direction.clone().multiply(1.5));

        createConeAoE(waveOrigin, furyWaveRange, furyWaveAngle, 2.5, direction, 60)
                .onEnter((aoe, entity) -> {
                    if (!entity.equals(caster) && !entity.equals(target) && Spellbook.canAttack(caster, entity)) {
                        // Deal damage instead of applying intimidation
                        entity.damage(Spellbook.getVariedAttributeBasedDamage(data, caster, entity, true, Attribute.ADVANTAGE_PHYSICAL), caster, PDamageType.PHYSICAL);
                        entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_SOUL_SAND_STEP, 0.5f, 0.8f);
                    }
                });

        caster.getWorld().playSound(waveOrigin, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.3f, 1.5f);

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= 12) {
                    this.cancel();
                    return;
                }

                double progress = ticks / 12.0;
                double currentRange = furyWaveRange * progress;

                for (int i = 0; i < 8; i++) {
                    double angle = (Math.PI * 2 * i / 8) + (ticks * 0.2);
                    double x = Math.cos(angle) * currentRange;
                    double z = Math.sin(angle) * currentRange;
                    Location particleLoc = waveOrigin.clone().add(x, 0.2, z);

                    waveOrigin.getWorld().spawnParticle(Particle.DUST, particleLoc, 1, 0, 0, 0, 0,
                        new Particle.DustOptions(org.bukkit.Color.MAROON, 1.5f));
                }

                ticks++;
            }
        }.runTaskTimer(Spellbook.getInstance().getImplementer(), 0L, 1L);
    }

    @Override
    protected void addSpellPlaceholders() {
        spellAddedPlaceholders.add(Component.text(Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_MAGICAL, furyMinDuration, furyMaxDuration, "furyDuration") / 20, VALUE_COLOR));
        placeholderNames.add("furyDuration");
    }
}
