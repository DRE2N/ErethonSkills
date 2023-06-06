package de.erethon.spellbook.effects;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellEffect;
import org.bukkit.entity.LivingEntity;

public class StabilityEffect extends SpellEffect {

    public StabilityEffect(EffectData data, LivingEntity caster, LivingEntity target, int duration, int stacks) {
        super(data, caster, target, duration, stacks);
    }

    @Override
    public boolean onAddEffect(SpellEffect effect, boolean isNew) {
        if (isNew && Spellbook.getInstance().getCCEffects().contains(effect.data)) {
            stacks -= 1;
            if (stacks <= 0) {
                ticksLeft = -1;
            }
            return false;
        }
        return super.onAddEffect(effect, isNew);
    }


}
