package de.erethon.spellbook.effects;

import de.erethon.spellbook.api.SpellEffect;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.entity.LivingEntity;

public class ResistanceEffect extends SpellTrait {

    public ResistanceEffect(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected boolean onAddEffect(SpellEffect effect, boolean isNew) {
        if (!effect.data.isPositive()) {
            return false;
        }
        return super.onAddEffect(effect, isNew);
    }
}
