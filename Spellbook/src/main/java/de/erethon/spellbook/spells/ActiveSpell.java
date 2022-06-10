package de.erethon.spellbook.spells;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.caster.SpellCaster;

public class ActiveSpell {

    Spellbook spellbook = Spellbook.getInstance();

    Spell spell;
    SpellCaster caster;

    public ActiveSpell(SpellCaster caster, Spell spell) {
        this.spell = spell;
        this.caster = caster;
    }

    public void cast() {

    }
}
