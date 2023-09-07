package de.erethon.spellbook.traits.assassin;

import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.SpellbookSpell;
import de.erethon.spellbook.api.TraitData;
import de.erethon.spellbook.spells.assassin.DaggerStorm;
import org.bukkit.entity.LivingEntity;

public class PointedBlades extends SpellTrait {

    private final double damageMultiplier = data.getDouble("damageMultiplier", 1.5);
    public PointedBlades(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected SpellbookSpell onSpellCast(SpellbookSpell cast) {
        if (cast instanceof DaggerStorm storm) {
            storm.damageMultiplier = damageMultiplier;
        }
        return super.onSpellCast(cast);
    }
}
