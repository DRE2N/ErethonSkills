package de.erethon.spellbook.traits.paladin;

import de.erethon.spellbook.api.SpellEffect;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.entity.LivingEntity;

public class Discipline extends SpellTrait {

    private final double ccMultiplier = data.getDouble("ccMultiplier", 0.8);

    public Discipline(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected boolean onAddEffect(SpellEffect effect, boolean isNew) {
        effect.
        return super.onAddEffect(effect, isNew);
    }
}
