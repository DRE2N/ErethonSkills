package de.erethon.spellbook.traits.warrior;

import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.SpellbookSpell;
import de.erethon.spellbook.api.TraitData;
import de.erethon.spellbook.spells.warrior.AbstractWarriorShout;
import org.bukkit.entity.LivingEntity;

public class LouderShouts extends SpellTrait {
    private final int rangeBonus = data.getInt("rangeBonus", 5);
    public LouderShouts(TraitData data, LivingEntity caster) {
        super(data, caster);
    }
    @Override
    protected void onSpellPreCast(SpellbookSpell spell) {
        if (spell instanceof AbstractWarriorShout shout) {
            shout.range += rangeBonus;
        }
    }
}
