package de.erethon.spellbook.spells.assassin.cutthroat;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.aoe.AoE;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellEffect;
import de.erethon.spellbook.combat.CombatSourceContext;
import de.erethon.spellbook.combat.BossControl;
import de.erethon.spellbook.spells.vfx.SkillSignatureVfx;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class PreciseStab extends CutthroatBaseSpell {

    // Perform a quick, piercing stab on the target. Deals moderate physical damage.
    // If the target has 3 or more Bleed stacks, this attack consumes the bleeding to grant you Fury.
    // When Fury is gained, creates a blood wave cone that damages nearby enemies.

    private final int range = data.getInt("range", 3);
    private final int bleedStacksRequired = data.getInt("bleedStacksRequired", 3);
    private final double furyWaveRange = data.getDouble("furyWaveRange", 6.0);
    private final double furyWaveAngle = data.getDouble("furyWaveAngle", 60.0);
    private final double focusedBonusPlayers = data.getDouble("focusedBonusPlayers", 0.3);
    private final double focusedBonusEntities = data.getDouble("focusedBonusEntities", 0.55);
    private final double waveDamageMultiplier = data.getDouble("waveDamageMultiplier", 0.65);
    private final int woundDuration = data.getInt("woundDuration", 7) * 20;

    private final EffectData bleedingEffectData = Spellbook.getEffectData("Bleeding");

    public PreciseStab(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = 60; // Animation duration
    }

    @Override
    protected boolean onPrecast() {
        return super.onPrecast() && lookForTarget(range);
    }

    @Override
    public boolean onCast() {
        if (!super.onCast()) {
            return false;
        }
        playPreciseStabBuildup();

        new BukkitRunnable() {
            @Override
            public void run() {
                executeStab();
            }
        }.runTaskLater(Spellbook.getInstance().getImplementer(), 8L); // 0.4 second delay

        return true;
    }

    private void playPreciseStabBuildup() {
        SkillSignatureVfx.accent(target.getEyeLocation(), getClass(), "lead", 1.8f, 9, false);
        caster.getWorld().playSound(caster.getLocation(), Sound.ITEM_CROSSBOW_LOADING_START, 0.7f, 1.8f);
    }
    private void executeStab() {
        if (target == null || !target.isValid() || target.isDead() || !caster.isValid()
                || caster.getWorld() != target.getWorld()
                || caster.getLocation().distanceSquared(target.getLocation()) > (range + 1) * (range + 1)
                || !caster.hasLineOfSight(target)) {
            caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_PLAYER_ATTACK_NODAMAGE, 0.6f, 0.8f);
            return;
        }
        int bleedingOnTarget = 0;
        SpellEffect bleedingEffect = null;
        for (SpellEffect effect : target.getEffects()) {
            if (effect.data == bleedingEffectData) {
                bleedingOnTarget += effect.getStacks();
                bleedingEffect = effect;
            }
        }

        boolean spendWound = bleedingOnTarget >= bleedStacksRequired && bleedingEffect != null;
        boolean wave = spendWound && caster instanceof Player player && player.isSneaking();
        double physicalDamage = Spellbook.getVariedAttributeBasedDamage(data, caster, target, true, Attribute.ADVANTAGE_PHYSICAL);
        if (spendWound && !wave) {
            double bonus = target instanceof org.bukkit.entity.Player ? focusedBonusPlayers : focusedBonusEntities;
            physicalDamage *= 1.0 + bonus * Math.min(bleedingOnTarget, 3);
        }
        double healthBefore = target.getHealth() + target.getAbsorptionAmount();
        double finalPhysicalDamage = physicalDamage;
        CombatSourceContext.runSpell(this, () -> target.damage(finalPhysicalDamage, caster, PDamageType.PHYSICAL));
        if (target.getHealth() + target.getAbsorptionAmount() >= healthBefore) {
            return;
        }
        BossControl.apply(caster, target, spendWound ? 20 : 10);
        playStrikeEffect();

        if (spendWound) {
            target.removeEffect(bleedingEffectData);
            if (wave) {
                createBloodWave();
            }

            caster.getWorld().playSound(target.getLocation(), Sound.BLOCK_CONDUIT_ATTACK_TARGET, 0.8f, 1.2f);
            caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_WITHER_HURT, 0.4f, 1.8f);
            target.getWorld().spawnParticle(Particle.BLOCK, target.getLocation(), 20, 0.5, 0.5, 0.5, 0.1, Material.REDSTONE_BLOCK.createBlockData());
        } else {
            target.addEffect(caster, bleedingEffectData, woundDuration, 1);
            caster.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.6f, 1.0f);
        }
    }

    private void playStrikeEffect() {
        Location strikeLocation = target.getLocation().add(0, 1, 0);
        SkillSignatureVfx.accent(strikeLocation, getClass(), "main", 2.2f, 17, false);
        caster.getWorld().playSound(strikeLocation, Sound.ITEM_CROSSBOW_SHOOT, 1.0f, 1.5f);
    }
    private void createBloodWave() {
        Vector direction = target.getLocation().subtract(caster.getLocation()).toVector().normalize();
        Location waveOrigin = caster.getLocation().add(direction.clone().multiply(1.5));

        createConeAoE(waveOrigin, furyWaveRange, furyWaveAngle, 2.5, direction, 60)
                .onEnter((aoe, entity) -> {
                    if (!entity.equals(caster) && !entity.equals(target) && Spellbook.canAttack(caster, entity)) {
                        double before = entity.getHealth() + entity.getAbsorptionAmount();
                        CombatSourceContext.runSpell(this, () -> entity.damage(Spellbook.getVariedAttributeBasedDamage(
                                data, caster, entity, true, Attribute.ADVANTAGE_PHYSICAL) * waveDamageMultiplier,
                                caster, PDamageType.PHYSICAL));
                        if (entity.getHealth() + entity.getAbsorptionAmount() < before) {
                            BossControl.apply(caster, entity, 10);
                        }
                        entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_SOUL_SAND_STEP, 0.5f, 0.8f);
                    }
                });

        caster.getWorld().playSound(waveOrigin, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.3f, 1.5f);
        SkillSignatureVfx.accent(waveOrigin.clone().add(direction.clone().multiply(furyWaveRange * 0.5)),
                getClass(), "wake", (float) furyWaveRange, 16, true);
    }
    @Override
    protected void addSpellPlaceholders() {
        spellAddedPlaceholders.add(Component.text(woundDuration / 20, VALUE_COLOR));
        placeholderNames.add("woundDuration");
    }
}
