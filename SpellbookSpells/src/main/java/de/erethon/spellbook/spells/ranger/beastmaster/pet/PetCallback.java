package de.erethon.spellbook.spells.ranger.beastmaster.pet;

import de.erethon.bedrock.chat.MessageUtil;
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
        RangerUtils.getPet(caster).callback();
        return true;
    }
}
