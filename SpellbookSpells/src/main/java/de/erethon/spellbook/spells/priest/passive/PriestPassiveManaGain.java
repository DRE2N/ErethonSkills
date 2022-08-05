package de.erethon.spellbook.spells.priest.passive;

import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.caster.SpellCaster;
import de.erethon.spellbook.spells.PassiveSpell;

/**
 * @author Fyreum
 */
public class PriestPassiveManaGain extends PassiveSpell {

    public PriestPassiveManaGain(SpellCaster caster, SpellData spellData) {
        super(caster, spellData);
        tickInterval = 10;
    }

    @Override
    protected boolean onCast() {
        caster.addEnergy(2);
        return true;
    }

    @Override
    protected void onTick() {
        onCast();
    }
}
