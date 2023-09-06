package de.erethon.spellbook.effects.paladin;

import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellEffect;
import org.bukkit.entity.LivingEntity;

public class JudgementOfGodEffect extends SpellEffect {
    public JudgementOfGodEffect(EffectData data, LivingEntity caster, LivingEntity target, int duration, int stacks) {
        super(data, caster, target, duration, stacks);
    }

    @Override
    public boolean onAddEffect(SpellEffect effect, boolean isNew) {
        if (effect.data.isPositive()) {
            return false;
        }
        return super.onAddEffect(effect, isNew);
    }
}
