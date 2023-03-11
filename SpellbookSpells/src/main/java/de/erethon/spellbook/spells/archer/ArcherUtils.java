package de.erethon.spellbook.spells.archer;

import de.erethon.spellbook.api.SpellData;
import org.bukkit.entity.LivingEntity;

public class ArcherUtils {

    public static boolean hasMana(LivingEntity caster, SpellData data) {
        boolean canCast = data.getInt("manaCost", 0) <= caster.getEnergy();
        if (!canCast) {
            caster.sendParsedActionBar("<color:#0000ff>Nicht genug Mana!");
        }
        return canCast;
    }
}
