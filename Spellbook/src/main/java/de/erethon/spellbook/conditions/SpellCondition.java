package de.erethon.spellbook.conditions;

import de.erethon.spellbook.SpellbookSpell;

public abstract class SpellCondition<T> {

    public abstract boolean check(SpellbookSpell spell);

}
