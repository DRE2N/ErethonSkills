package de.erethon.spellbook.spells.paladin.inquisitor;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellEffect;
import de.erethon.spellbook.spells.paladin.PaladinBaseSpell;
import org.bukkit.entity.LivingEntity;

public class InquisitorBaseSpell extends PaladinBaseSpell {

    protected final EffectData judgementData = Spellbook.getEffectData("Judgement");

    public InquisitorBaseSpell(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
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

    protected void addJudgement(LivingEntity target) {
        if (target == null) return;
        target.addEffect(caster, judgementData, Integer.MAX_VALUE, 1);
    }

    protected void removeJudgement(LivingEntity target) {
        if (target == null) return;
        target.removeEffect(judgementData);
    }


}
