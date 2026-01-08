package de.erethon.hecate.ui;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.hecate.Hecate;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class EntityStatusDisplayManager {

    private final HashMap<Entity, EntityStatusDisplay> displays = new HashMap<>();
    private final Set<EntityStatusDisplay> tickingStatusDisplays = new HashSet<>();
    public static NamespacedKey ENTITY_STATUS_KEY = new NamespacedKey("spellbook", "status");

    public EntityStatusDisplayManager() {
        BukkitRunnable updateNames = new BukkitRunnable() {
            @Override
            public void run() {
                displays.values().forEach(EntityStatusDisplay::updateDisplayName); // Make sure third-party plugin name updates are respected as well
            }
        };
        updateNames.runTaskTimer(Hecate.getInstance(), 0, 1200);
        BukkitRunnable tickDisplays = new BukkitRunnable() {
            @Override
            public void run() {
                Set<EntityStatusDisplay> copy = new HashSet<>(tickingStatusDisplays);
                for (EntityStatusDisplay display : copy) {
                    display.onTick();
                }
            }
        };
        tickDisplays.runTaskTimer(Hecate.getInstance(), 0, 20);
    }

    public boolean hasStatusDisplay(Entity entity) {
        return displays.containsKey(entity);
    }

    public void addStatusDisplay(Entity entity, EntityStatusDisplay display) {
        displays.put(entity, display);
    }

    public void markForTicking(EntityStatusDisplay display) {
        tickingStatusDisplays.add(display);
    }

    public void unmarkForTicking(EntityStatusDisplay display) {
        tickingStatusDisplays.remove(display);
    }

    public void removeStatusDisplay(Entity entity) {
        BukkitRunnable later = new BukkitRunnable() {
            @Override
            public void run() {
                if (!displays.containsKey(entity)) {
                    return;
                }
                displays.get(entity).remove();
                displays.remove(entity);
            }
        };
        later.runTaskLater(Hecate.getInstance(), 1); // Don't remove entities in the remove event
    }

    public EntityStatusDisplay getStatusDisplay(Entity entity) {
        return displays.get(entity);
    }

}
