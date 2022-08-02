package de.erethon.spellbook.mechanics;

import de.erethon.spellbook.conditions.SpellCondition;
import de.erethon.spellbook.SpellbookSpell;
import de.erethon.spellbook.targeters.SpellTargeter;

import java.util.ArrayList;
import java.util.List;

public abstract class SpellMechanic {

    List<SpellCondition> conditions = new ArrayList<>();
    List<SpellTargeter> targets = new ArrayList<>();

    public abstract void execute(SpellbookSpell spell);
}
