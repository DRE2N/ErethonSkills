package de.erethon.hecate.ui;

import de.erethon.hecate.Hecate;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.EntityDataSerializers;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class EntityStatusDisplayManager {

    private final HashMap<Entity, EntityStatusDisplay> displays = new HashMap<>();
    private final Set<EntityStatusDisplay> tickingStatusDisplays = new HashSet<>();
    private final Set<EntityStatusDisplay> pendingHealthUpdates = new HashSet<>();
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
                for (EntityStatusDisplay display : displays.values()) {
                    display.updateLayout();
                    display.updateHealthDisplay();
                }
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

    public void queueHealthUpdate(EntityStatusDisplay display) {
        if (pendingHealthUpdates.add(display) && pendingHealthUpdates.size() == 1) {
            Bukkit.getScheduler().runTask(Hecate.getInstance(), () -> {
                pendingHealthUpdates.forEach(EntityStatusDisplay::updateHealthDisplay);
                pendingHealthUpdates.clear();
            });
        }
    }

    public void refreshPackText(Player viewer) {
        for (EntityStatusDisplay display : displays.values()) {
            resendText(viewer, display.healthDisplay);
            resendText(viewer, display.entityNameTag);
        }
    }

    private void resendText(Player viewer, TextDisplay display) {
        if (!display.isValid() || !display.isTrackedBy(viewer)) {
            return;
        }
        // Receiving text metadata invalidates the client's cached layout after a pack reload.
        var textData = ((CraftEntity) display).getHandle().getEntityData().packAll().stream()
                .filter(value -> value.serializer() == EntityDataSerializers.COMPONENT)
                .toList();
        ((CraftPlayer) viewer).getHandle().connection.send(new ClientboundSetEntityDataPacket(display.getEntityId(), textData));
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
