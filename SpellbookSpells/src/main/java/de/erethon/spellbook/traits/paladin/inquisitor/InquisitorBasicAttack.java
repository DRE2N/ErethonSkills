package de.erethon.spellbook.traits.paladin.inquisitor;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellEffect;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;

public class InquisitorBasicAttack extends SpellTrait {

    // The Inquisitor's basic attack applies a "Judgement" effect to the target, stacking up to a maximum number of stacks.

    private final int maximumJudgementStacks = data.getInt("maximumJudgementStacks", 7);

    protected final EffectData judgementData = Spellbook.getEffectData("Judgement");

    public InquisitorBasicAttack(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    public double onAttack(LivingEntity target, double damage, PDamageType type) {
        int judgementStacks = getJudgementStacksOnTarget(target);
        if (judgementStacks < maximumJudgementStacks) {
            target.addEffect(caster, judgementData, Integer.MAX_VALUE, 1);
        }
        if (judgementStacks == maximumJudgementStacks) {
            target.getWorld().playSound(target, Sound.BLOCK_CONDUIT_ACTIVATE, 0.7f, 0.5f);
        }
        return super.onAttack(target, damage, type);
    }

    protected int getJudgementStacksOnTarget(LivingEntity target) {
        if (target == null) return 0;
        SpellEffect judgementEffect = null;
        for (SpellEffect effect : target.getEffects()) {
            if (effect.data == judgementData) {
                judgementEffect = effect;
                break;
            }
        }
        if (judgementEffect == null) return 0;
        return judgementEffect.getStacks();
    }
}
