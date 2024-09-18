package de.erethon.spellbook.fx.cues;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.utils.TransformationUtil;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public record SweepingBlockDisplayCue(Material material, int count, int duration, int teleportDuration, float scale) implements FXCue {

    private static final Map<LivingEntity, BukkitRunnable> displays = new HashMap<>();

    // A bunch of block displays rotating around the player in a circle
    @Override
    public void run(LivingEntity trigger) {
        if (displays.containsKey(trigger)) {
            // Task is already running for this entity, do not schedule another one
            return;
        }
        Set<BlockDisplay> activeDisplays = new HashSet<>();
        for (int i = 0; i < count; i++) {
            BlockDisplay display = trigger.getWorld().spawn(trigger.getLocation(), BlockDisplay.class, blockDisplay -> {
                blockDisplay.setBlock(material.createBlockData());
                blockDisplay.setPersistent(false);
                blockDisplay.setRotation(0, 0);
                blockDisplay.setTeleportDuration(teleportDuration);
                blockDisplay.setTransformation(TransformationUtil.scale(blockDisplay.getTransformation(), scale));
            });
            activeDisplays.add(display);
        }
        BukkitRunnable task = new BukkitRunnable() {
            private int currentTick = 0;
            @Override
            public void run() {
                Vector vector = trigger.getLocation().toVector();
                for (BlockDisplay display : activeDisplays) {
                    Vector newVector = new Vector(Math.cos(currentTick * Math.PI / 10) * 2, 0, Math.sin(currentTick * Math.PI / 10) * 2);
                    display.teleport(vector.add(newVector).toLocation(trigger.getWorld()));
                }
                currentTick++;
            }
        };
        task.runTaskTimer(Spellbook.getInstance().getImplementer(), 0, teleportDuration);
        displays.put(trigger, task);
    }

    @Override
    public boolean isOnceAndDone() {
        return false;
    }

    @Override
    public int getDelay() {
        return duration;
    }

    @Override
    public void onRemove(LivingEntity trigger) {
        displays.get(trigger).cancel();
        displays.remove(trigger);
    }
}
