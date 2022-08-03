package de.erethon.spellbook.spells.mage;

import de.erethon.spellbook.SpellData;
import de.erethon.spellbook.SpellbookSpell;
import de.erethon.spellbook.caster.SpellCaster;

/**
 * @author Fyreum
 */
public class MageBaseSpell extends SpellbookSpell {

    protected final int manaCost;

    public MageBaseSpell(SpellCaster caster, SpellData spellData) {
        super(caster, spellData);
        manaCost = spellData.getInt("manaCost", 10);
    }

    @Override
    protected boolean onPrecast() {
        boolean canCast = manaCost <= caster.getEnergy();
        if (!canCast) {
            caster.sendActionbar("<red>Nicht genug Mana!");
        }
        return canCast;
    }
}
