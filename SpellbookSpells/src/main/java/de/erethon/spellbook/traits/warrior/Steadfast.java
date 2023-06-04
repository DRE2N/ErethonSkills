package de.erethon.spellbook.traits.warrior;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellEffect;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.entity.LivingEntity;

public class Steadfast extends SpellTrait {

    private final double durationMultiplier = data.getDouble("durationMultiplier", 0.8);
    private final int stackReduction = data.getInt("stackReduction", 1);

    public Steadfast(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected boolean onAddEffect(SpellEffect originalEffect, boolean isNew) {
        if (isNew && Spellbook.getInstance().getCCEffects().contains(originalEffect.data)) {
            caster.addEffect(originalEffect.getCaster(), originalEffect.data, (int) Math.floor(originalEffect.getTicksLeft() * durationMultiplier), Math.min(1, originalEffect.getStacks() - stackReduction));
            return false;
        }
        return super.onAddEffect(originalEffect, isNew);
    }
}
