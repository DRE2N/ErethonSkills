package de.erethon.spellbook.spells.ranger.pet;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookSpell;
import de.erethon.spellbook.spells.ranger.RangerBaseSpell;
import de.erethon.spellbook.utils.RangerUtils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

public class PetMakeAttack extends RangerBaseSpell {

    private final int range = data.getInt("range", 32);
    private LivingEntity target;

    public PetMakeAttack(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onPrecast() {
        if (!RangerUtils.hasPet(caster)) return false;
        return lookForTarget(true);
    }

    @Override
    protected boolean onCast() {
       RangerUtils.getPet(caster).makeAttack(target);
       return true;
    }
}
