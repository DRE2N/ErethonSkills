package de.erethon.spellbook.conditions;

import de.erethon.spellbook.ActiveSpell;

public abstract class SpellCondition<T> {

    public abstract boolean check(ActiveSpell spell);

}
