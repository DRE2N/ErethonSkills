package de.erethon.spellbook.utils;

import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellbookSpell;
import de.erethon.spellbook.api.SpellData;
import org.bukkit.entity.LivingEntity;

public abstract class AssassinUtils {

    public static boolean hasEnergy(LivingEntity caster, SpellData data) {
        boolean canCast = data.getInt("energyCost", 0) <= caster.getEnergy();
        if (!canCast) {
            caster.sendParsedActionBar("<color:#ff0000>Nicht genug Energie!");
        }
        return canCast;
    }

}

