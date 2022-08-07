package de.erethon.spellbook.spells.assassin;

import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellbookSpell;
import de.erethon.spellbook.api.SpellData;
import org.bukkit.entity.LivingEntity;

public abstract class AssassinUtils {

    public static boolean hasEnergy(LivingEntity caster, SpellData data) {
        boolean canCast = data.getInt("energyCost", 0) <= caster.getEnergy();
        if (!canCast) {
            caster.sendActionbar("<color:#ff0000>Nicht genug Energie!");
        }
        return canCast;
    }

}

