package de.erethon.spellbook.utils;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.ranger.pet.RangerPet;
import net.minecraft.server.MinecraftServer;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

public class RangerUtils {

    private static final NamespacedKey PET_KEY = new NamespacedKey("spellbook", "pet");

    public static boolean hasMana(LivingEntity caster, SpellData data) {
        boolean canCast = data.getInt("manaCost", 0) <= caster.getEnergy();
        if (!canCast) {
            caster.sendParsedActionBar("<color:#0000ff>Nicht genug Mana!");
        }
        return canCast;
    }

    public static boolean hasPet(LivingEntity caster) {
        if (Spellbook.getInstance().getPetLookup().get(caster) != null) {
            return true;
        }
        caster.sendParsedActionBar("<color:#ff0000>Kein Pet aktiv!");
        return false;
    }
}
