package de.erethon.spellbook.utils;

import com.magmaguy.freeminecraftmodels.customentity.VFXEntity;
import de.erethon.spellbook.Spellbook;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

public class VFXUtil {

    public static VFXEntity spawn(Location location, String id) {
        if (!Bukkit.getPluginManager().isPluginEnabled("Daedalus")) {
            Spellbook.log("Daedalus plugin is not enabled. Cannot spawn VFX entity.");
            return null;
        }
        VFXEntity entity = VFXEntity.create(id, location);
        if (entity == null) {
            Spellbook.log("Failed to spawn VFX entity with id: " + id);
            return null;
        }
        return entity;
    }

    public static VFXEntity spawn(LivingEntity livingEntity, String id) {
        if (!Bukkit.getPluginManager().isPluginEnabled("Daedalus")) {
            Spellbook.log("Daedalus plugin is not enabled. Cannot spawn VFX entity.");
            return null;
        }
        VFXEntity entity =VFXEntity.createMounted(id, livingEntity);
        if (entity == null) {
            Spellbook.log("Failed to spawn mounted VFX entity with id: " + id);
            return null;
        }
        return entity;
    }
}
