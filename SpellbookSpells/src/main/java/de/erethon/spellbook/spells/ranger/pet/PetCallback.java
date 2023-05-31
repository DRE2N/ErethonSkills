package de.erethon.spellbook.spells.ranger.pet;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookSpell;
import de.erethon.spellbook.utils.RangerUtils;
import org.bukkit.entity.LivingEntity;

public class PetCallback extends SpellbookSpell {

    public PetCallback(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onPrecast() {
        if (!RangerUtils.hasPet(caster)) return false;
        return true;
    }

    @Override
    protected boolean onCast() {
        Spellbook.getInstance().getPetLookup().get(caster).callback();
        return true;
    }
}
