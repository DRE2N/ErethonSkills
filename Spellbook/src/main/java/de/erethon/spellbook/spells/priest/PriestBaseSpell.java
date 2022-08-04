package de.erethon.spellbook.spells.priest;

import de.erethon.spellbook.SpellData;
import de.erethon.spellbook.SpellbookSpell;
import de.erethon.spellbook.caster.SpellCaster;

/**
 * @author Fyreum
 */
public class PriestBaseSpell extends SpellbookSpell {

    protected final int manaCost;

    public PriestBaseSpell(SpellCaster caster, SpellData spellData) {
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
