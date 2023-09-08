package de.erethon.spellbook.traits.warrior;

import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.SpellbookSpell;
import de.erethon.spellbook.api.TraitData;
import de.erethon.spellbook.spells.warrior.MightyBlow;
import org.bukkit.entity.LivingEntity;

public class MightySwing extends SpellTrait {

    private final double rangeMultiplierMultiplier = data.getDouble("rangeMultiplierMultiplier", 1.5);

    public MightySwing(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected SpellbookSpell onSpellCast(SpellbookSpell cast) {
        if (cast instanceof MightyBlow spell) {
            spell.rangeMultiplier *= rangeMultiplierMultiplier;
        }
        return super.onSpellCast(cast);
    }
}
