package de.erethon.spellbook.spells.mage.passive;

import de.erethon.spellbook.SpellData;
import de.erethon.spellbook.caster.SpellCaster;
import de.erethon.spellbook.spells.PassiveSpell;

/**
 * @author Fyreum
 */
public class MagePassiveManaGain extends PassiveSpell {

    public MagePassiveManaGain(SpellCaster caster, SpellData spellData) {
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
