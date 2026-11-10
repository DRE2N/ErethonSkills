package de.erethon.spellbook.spells.assassin.cutthroat;

import de.erethon.spellbook.utils.SpellbookFeedback;

import de.erethon.spellbook.Spellbook;
import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.combat.CombatSourceContext;
import de.erethon.spellbook.combat.BossControl;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellEffect;
import de.erethon.spellbook.spells.vfx.SkillSignatureVfx;
import de.erethon.spellbook.utils.SpellbookCommonMessages;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class Mangle extends CutthroatBaseSpell {

    // A committed strike. Wound raises damage and can interrupt an active player cast.

    private final int range = data.getInt("range", 3);
    private final int bleedStacksRequired = data.getInt("bleedStacksRequired", 3);
    private final double bonusDamagePerStack = data.getDouble("bonusDamagePerStack", 0.15);

    private final EffectData bleedingEffectData = Spellbook.getEffectData("Bleeding");
    private final EffectData silenceEffectData = Spellbook.getEffectData("Silence");

    public Mangle(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onPrecast() {
        lookForTarget(range);
        if (target == null) {
            SpellbookFeedback.send(caster, SpellbookCommonMessages.NO_TARGET);
            return false;
        }
        return super.onPrecast();
    }

    @Override
    public boolean onCast() {
        if (!super.onCast()) {
            return false;
        }
        playMangleWindup();

        new BukkitRunnable() {
            @Override
            public void run() {
                executeMangle();
            }
        }.runTaskLater(Spellbook.getInstance().getImplementer(), 10L);

        return true;
    }

    private void playMangleWindup() {
        Location targetLoc = target.getLocation().add(0, 1, 0);
        SkillSignatureVfx.accent(target.getLocation(), getClass(), "lead", 3.2f, 12, true);
        SkillSignatureVfx.accent(targetLoc, getClass(), "wake", 2.0f, 12, false);
        target.getWorld().playSound(targetLoc, Sound.BLOCK_GRINDSTONE_USE, 0.5f, 0.8f);
    }

    private void executeMangle() {
        if (target == null || !target.isValid() || target.isDead() || !caster.isValid()
                || target.getWorld() != caster.getWorld()
                || target.getLocation().distanceSquared(caster.getLocation()) > (range + 1) * (range + 1)
                || !caster.hasLineOfSight(target)) {
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

        double baseDamage = Spellbook.getVariedAttributeBasedDamage(data, caster, target, true, Attribute.ADVANTAGE_PHYSICAL);

        if (bleedingOnTarget >= bleedStacksRequired && bleedingEffect != null) {
            executeBloodiedMangle(bleedingOnTarget, bleedingEffect, baseDamage);
        } else {
            executeRegularMangle(baseDamage);
        }
    }

    private void executeBloodiedMangle(int bleedingStacks, SpellEffect bleedingEffect, double baseDamage) {
        int consumedStacks = Math.min(bleedingStacks, bleedStacksRequired + 3);
        double enhancedDamage = baseDamage * (1 + (consumedStacks * bonusDamagePerStack));
        double before = target.getHealth() + target.getAbsorptionAmount();
        CombatSourceContext.runSpell(this, () -> target.damage(enhancedDamage, caster, PDamageType.PHYSICAL));
        if (target.getHealth() + target.getAbsorptionAmount() >= before) {
            return;
        }
        int remainingStacks = bleedingStacks - consumedStacks;
        if (remainingStacks <= 0) {
            target.removeEffect(bleedingEffectData);
        } else {
            bleedingEffect.setStacks(remainingStacks);
        }
        if (target instanceof org.bukkit.entity.Player && !target.getActiveSpells().isEmpty()) {
            for (var spell : target.getActiveSpells().toArray(de.erethon.spellbook.api.SpellbookSpell[]::new)) {
                spell.interrupt();
            }
            target.addEffect(caster, silenceEffectData, 10, 1);
        }
        BossControl.apply(caster, target, 70);
        playDevastatingEffects();
    }

    private void executeRegularMangle(double baseDamage) {
        double before = target.getHealth() + target.getAbsorptionAmount();
        CombatSourceContext.runSpell(this, () -> target.damage(baseDamage, caster, PDamageType.PHYSICAL));
        if (target.getHealth() + target.getAbsorptionAmount() >= before) return;
        BossControl.apply(caster, target, 25);
        Location targetLoc = target.getLocation().add(0, 1, 0);
        caster.getWorld().playSound(targetLoc, Sound.BLOCK_BAMBOO_BREAK, 1.0f, 0.5f);
        SkillSignatureVfx.accent(targetLoc, getClass(), "wake", 1.9f, 15, false);
    }

    private void playDevastatingEffects() {
        Location targetLoc = target.getLocation().add(0, 1, 0);
        SkillSignatureVfx.accent(targetLoc, getClass(), "main", 3.6f, 24, false);
        SkillSignatureVfx.accent(target.getLocation(), getClass(), "wake", 4.0f, 24, true);
        target.getWorld().playSound(targetLoc, Sound.ENTITY_WITHER_HURT, 0.6f, 1.2f);
    }
}