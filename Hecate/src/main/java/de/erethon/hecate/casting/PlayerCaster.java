package de.erethon.hecate.casting;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.caster.SpellCaster;
import de.erethon.spellbook.spells.Spell;
import org.bukkit.entity.LivingEntity;

import java.util.HashSet;
import java.util.Set;

public class PlayerCaster extends SpellCaster {

    private Set<Spell> unlockedSpells = new HashSet<>();
    private Spell[] assignedSlots = new Spell[8];

    public PlayerCaster(Spellbook spellbook, LivingEntity entity) {
        super(spellbook, entity);
    }

    public Set<Spell> getUnlockedSpells() {
        return unlockedSpells;
    }

    public Spell getSpellAt(int slot) {
        return assignedSlots[slot];
    }
}
