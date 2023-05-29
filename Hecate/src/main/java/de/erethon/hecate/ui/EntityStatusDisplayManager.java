package de.erethon.hecate.ui;

import de.erethon.hecate.Hecate;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;

public class EntityStatusDisplayManager {

    private final HashMap<Entity, EntityStatusDisplay> displays = new HashMap<>();
    public static NamespacedKey statusKey = new NamespacedKey("spellbook", "status");

    public EntityStatusDisplayManager() {
        BukkitRunnable updateNames = new BukkitRunnable() {
            @Override
            public void run() {
                displays.values().forEach(EntityStatusDisplay::updateDisplayName); // Make sure third-party plugin name updates are respected as well
            }
        };
        updateNames.runTaskTimer(Hecate.getInstance(), 0, 1200);
    }

    public boolean hasStatusDisplay(Entity entity) {
        return displays.containsKey(entity);
    }

    public void addStatusDisplay(Entity entity, EntityStatusDisplay display) {
        displays.put(entity, display);
    }

    public void removeStatusDisplay(Entity entity) {
        displays.get(entity).remove();
        displays.remove(entity);
    }

    public EntityStatusDisplay getStatusDisplay(Entity entity) {
        return displays.get(entity);
    }

}
