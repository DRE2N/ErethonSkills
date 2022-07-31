package de.erethon.spellbook.spells;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.caster.SpellCaster;

public class TestSpell extends SpellData {

    public TestSpell(Spellbook spellbook, String id) {
        super(spellbook, id);
    }

    @Override
    public boolean precast(SpellCaster caster, ActiveSpell activeSpell) {
        return true;
    }

    @Override
    public boolean cast(SpellCaster caster, ActiveSpell activeSpell) {
        return true;
    }

    @Override
    public void afterCast(SpellCaster caster, ActiveSpell activeSpell) {

    }
}
