package de.erethon.spellbook.spells;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.caster.SpellCaster;

import java.util.UUID;

public class ActiveSpell {

    Spellbook spellbook = Spellbook.getInstance();
    private UUID uuid;

    private Spell spell;
    private SpellCaster caster;

    public ActiveSpell(SpellCaster caster, Spell spell) {
        this.spell = spell;
        this.caster = caster;
    }

    public void cast() {
        uuid = UUID.randomUUID();
    }

    public UUID getUuid() {
        return uuid;
    }

    public Spell getSpell() {
        return spell;
    }

    public SpellCaster getCaster() {
        return caster;
    }
}
