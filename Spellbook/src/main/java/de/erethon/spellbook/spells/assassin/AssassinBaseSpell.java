package de.erethon.spellbook.spells.assassin;

import de.erethon.spellbook.SpellbookSpell;
import de.erethon.spellbook.SpellData;
import de.erethon.spellbook.caster.SpellCaster;

public abstract class AssassinBaseSpell extends SpellbookSpell {

    protected final int energyCost;

    public AssassinBaseSpell(SpellCaster caster, SpellData spellData) {
        super(caster, spellData);
        energyCost = spellData.getInt("energyCost", 0);
    }

    @Override
    protected boolean onPrecast() {
        boolean canCast = energyCost <= caster.getEnergy();
        if (!canCast) {
            caster.sendActionbar("<red>Nicht genug Energie!");
        }
        return canCast;
    }

    @Override
    protected void onAfterCast() {
        caster.removeEnergy(energyCost);
    }

}

