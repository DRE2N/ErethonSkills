package de.erethon.spellbook.spells.assassin.cutthroat;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.aoe.AoE;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellEffect;
import de.erethon.spellbook.spells.assassin.AssassinBaseSpell;
import de.erethon.spellbook.utils.SpellbookCommonMessages;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.CircleEffect;
import de.slikey.effectlib.effect.SphereEffect;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;

public class Mangle extends CutthroatBaseSpell {

    // Powerful execution attack that silences and weakens a target if they are bleeding.
    // Creates a terror aura that spreads fear to nearby enemies and leaves lingering wounds.
    // The more bleeding stacks consumed, the more devastating the effect.

    private final int range = data.getInt("range", 3);
    private final int bleedStacksRequired = data.getInt("bleedStacksRequired", 3);
    private final int silenceMinDuration = data.getInt("silenceMinDuration", 8) * 20;
    private final int silenceMaxDuration = data.getInt("silenceMaxDuration", 16) * 20;
    private final int weaknessMinDuration = data.getInt("weaknessMinDuration", 10) * 20;
    private final int weaknessMaxDuration = data.getInt("weaknessMaxDuration", 25) * 20;
    private final double terrorAuraRadius = data.getDouble("terrorAuraRadius", 5.0);
    private final int fearDuration = data.getInt("fearDuration", 4) * 20;
    private final double bonusDamagePerStack = data.getDouble("bonusDamagePerStack", 0.15);
    private final int woundDuration = data.getInt("woundDuration", 8) * 20;

    private final EffectData bleedingEffectData = Spellbook.getEffectData("Bleeding");
    private final EffectData silenceEffectData = Spellbook.getEffectData("Silence");
    private final EffectData weaknessEffectData = Spellbook.getEffectData("Weakness");
    private final EffectData slowEffectData = Spellbook.getEffectData("Slow");

    public Mangle(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onPrecast() {
        lookForTarget(range);
        if (target == null) {
            caster.sendParsedActionBar(SpellbookCommonMessages.NO_TARGET);
            return false;
        }
        return super.onPrecast();
    }

    @Override
    public boolean onCast() {
        playMangleWindup();

        new BukkitRunnable() {
            @Override
            public void run() {
                executeMangle();
            }
        }.runTaskLater(Spellbook.getInstance().getImplementer(), 15L); // 0.75 second wind-up

        return super.onCast();
    }

    private void playMangleWindup() {
        Location targetLoc = target.getLocation().add(0, 1, 0);

        target.getWorld().spawnParticle(Particle.SMOKE, targetLoc, 15, 0.5, 0.5, 0.5, 0.02);
        target.getWorld().spawnParticle(Particle.ENCHANTED_HIT, targetLoc, 8, 0.3, 0.3, 0.3, 0.1);

        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 0.3f, 1.8f);
        target.getWorld().playSound(targetLoc, Sound.BLOCK_GRINDSTONE_USE, 0.5f, 0.8f);

        EffectManager effectManager = Spellbook.getInstance().getEffectManager();
        if (effectManager != null) {
            CircleEffect doomCircle = new CircleEffect(effectManager);
            doomCircle.setLocation(targetLoc);
            doomCircle.radius = 1.5f;
            doomCircle.particle = Particle.DUST;
            doomCircle.particles = 20;
            doomCircle.duration = 15;
            doomCircle.start();
        }
    }

    private void executeMangle() {
        int bleedingOnTarget = 0;
        SpellEffect bleedingEffect = null;
        for (SpellEffect effect : target.getEffects()) {
            if (effect.data == bleedingEffectData) {
                bleedingOnTarget += effect.getStacks();
                bleedingEffect = effect;
            }
        }

        double baseDamage = Spellbook.getVariedAttributeBasedDamage(data, caster, target, true, Attribute.ADVANTAGE_PHYSICAL);

        if (bleedingOnTarget >= bleedStacksRequired && bleedingEffect != null) {
            executeBloodiedMangle(bleedingOnTarget, bleedingEffect, baseDamage);
        } else {
            executeRegularMangle(baseDamage);
        }
    }

    private void executeBloodiedMangle(int bleedingStacks, SpellEffect bleedingEffect, double baseDamage) {
        int consumedStacks = Math.min(bleedingStacks, bleedStacksRequired + 3);
        int remainingStacks = bleedingStacks - consumedStacks;

        if (remainingStacks <= 0) {
            target.removeEffect(bleedingEffectData);
        } else {
            bleedingEffect.setStacks(remainingStacks);
        }

        double enhancedDamage = baseDamage * (1 + (consumedStacks * bonusDamagePerStack));
        target.damage(enhancedDamage, caster);

        int silenceDuration = (int) Spellbook.getRangedValue(data, caster, target, Attribute.ADVANTAGE_MAGICAL, silenceMinDuration, silenceMaxDuration, "silenceDuration");
        int weaknessDuration = (int) Spellbook.getRangedValue(data, caster, target, Attribute.ADVANTAGE_MAGICAL, weaknessMinDuration, weaknessMaxDuration, "weaknessDuration");

        target.addEffect(caster, silenceEffectData, silenceDuration, 1);
        target.addEffect(caster, weaknessEffectData, weaknessDuration, 1);

        if (slowEffectData != null) {
            target.addEffect(caster, slowEffectData, woundDuration, Math.min(consumedStacks / 2, 3));
        }

        createTerrorAura();

        playDevastatingEffects();
    }

    private void executeRegularMangle(double baseDamage) {
        target.damage(baseDamage, caster);
        Location targetLoc = target.getLocation().add(0, 1, 0);
        caster.getWorld().playSound(targetLoc, Sound.BLOCK_BAMBOO_BREAK, 1.0f, 0.5f);
        target.getWorld().spawnParticle(Particle.ITEM_SLIME, targetLoc, 3, 0.5, 0.5, 0.5, 0);
        target.getWorld().spawnParticle(Particle.CRIT, targetLoc, 5, 0.3, 0.3, 0.3, 0.1);
    }

    private void createTerrorAura() {
        double baseDamage = Spellbook.getVariedAttributeBasedDamage(data, caster, target, true, Attribute.ADVANTAGE_PHYSICAL);

        createCircularAoE(target.getLocation(), terrorAuraRadius, 2.0, 80)
                .onEnter((aoe, entity) -> {
                    if (!entity.equals(caster) && !entity.equals(target) && Spellbook.canAttack(caster, entity)) {
                        if (slowEffectData != null) {
                            entity.addEffect(caster, slowEffectData, fearDuration, 2);
                        }

                        double terrorDamage = baseDamage * 0.2;
                        entity.damage(terrorDamage, caster);

                        entity.getWorld().spawnParticle(Particle.SOUL, entity.getLocation().add(0, 1, 0), 8, 0.4, 0.4, 0.4, 0.05);
                        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_GHAST_AMBIENT, 0.3f, 1.5f);
                    }
                });
    }

    private void playDevastatingEffects() {
        Location targetLoc = target.getLocation().add(0, 1, 0);

        target.getWorld().spawnParticle(Particle.BLOCK, targetLoc, 30, 0.8, 0.8, 0.8, 0.1, Material.REDSTONE_BLOCK.createBlockData());
        target.getWorld().spawnParticle(Particle.ITEM_SLIME, targetLoc, 15, 0.6, 0.6, 0.6, 0.1);
        target.getWorld().spawnParticle(Particle.SMOKE, targetLoc, 20, 0.5, 0.5, 0.5, 0.1);
        target.getWorld().spawnParticle(Particle.ENCHANTED_HIT, targetLoc, 12, 0.4, 0.4, 0.4, 0.2);

        caster.getWorld().playSound(targetLoc, Sound.BLOCK_BAMBOO_BREAK, 1.2f, 1.2f);
        caster.getWorld().playSound(targetLoc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 0.8f, 0.8f);
        caster.getWorld().playSound(targetLoc, Sound.ENTITY_WITHER_HURT, 0.4f, 1.5f);

        EffectManager effectManager = Spellbook.getInstance().getEffectManager();
        if (effectManager != null) {
            SphereEffect shockwave = new SphereEffect(effectManager);
            shockwave.setLocation(targetLoc);
            shockwave.radius = 3.0f;
            shockwave.particle = Particle.DUST;
            shockwave.particles = 40;
            shockwave.duration = 20;
            shockwave.start();
        }

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= 40 || target.isDead()) {
                    this.cancel();
                    return;
                }

                if (ticks % 10 == 0) {
                    target.getWorld().spawnParticle(Particle.DUST, targetLoc, 5, 0.3, 0.3, 0.3, 0,
                        new Particle.DustOptions(org.bukkit.Color.MAROON, 1.0f));
                    target.getWorld().playSound(targetLoc, Sound.BLOCK_SOUL_SAND_STEP, 0.2f, 0.8f);
                }

                ticks++;
            }
        }.runTaskTimer(Spellbook.getInstance().getImplementer(), 0L, 1L);
    }

    @Override
    protected void addSpellPlaceholders() {
        spellAddedPlaceholders.add(Component.text(Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_MAGICAL, silenceMinDuration, silenceMaxDuration, "silenceDuration") / 20, VALUE_COLOR));
        placeholderNames.add("silenceDuration");
        spellAddedPlaceholders.add(Component.text(Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_MAGICAL, weaknessMinDuration, weaknessMaxDuration, "weaknessDuration") / 20, VALUE_COLOR));
        placeholderNames.add("weaknessDuration");
        super.addSpellPlaceholders();
    }
}
