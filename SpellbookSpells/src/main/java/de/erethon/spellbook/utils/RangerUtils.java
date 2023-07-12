package de.erethon.spellbook.utils;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.papyrus.DamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.ranger.pet.RangerPet;
import net.minecraft.server.MinecraftServer;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

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

    public static RangerPet getPet(LivingEntity caster) {
        return Spellbook.getInstance().getPetLookup().get(caster);
    }

    public static AbstractArrow sendProjectile(LivingEntity start, LivingEntity target, LivingEntity shooter, double speed, double damage, DamageType damageType) {
        Vector from = start.getLocation().toVector();
        Vector to = target.getLocation().toVector();
        Vector direction = to.subtract(from);
        direction = direction.normalize();
        direction.multiply(speed);
        AbstractArrow proj = start.launchProjectile(Arrow.class, direction);
        //proj.setGravity(false);
        proj.setShooter(shooter);
        proj.setDamageType(damageType);
        proj.setDamage(damage);
        return proj;
    }
}
