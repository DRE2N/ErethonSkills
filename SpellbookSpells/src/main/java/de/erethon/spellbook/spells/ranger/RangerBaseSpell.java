package de.erethon.spellbook.spells.ranger;

import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookSpell;
import org.bukkit.entity.LivingEntity;

public class RangerBaseSpell extends SpellbookSpell {

    protected final int manaCost;


    public RangerBaseSpell(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        manaCost = spellData.getInt("manaCost", 10);
    }

    @Override
    protected boolean onPrecast() {
        boolean canCast = manaCost <= caster.getEnergy();
        if (!canCast) {
            caster.sendParsedActionBar("<color:#ff0000>Nicht genug Mana!");
        }
        return canCast;
    }
}
