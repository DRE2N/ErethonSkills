package de.erethon.spellbook.fx.cues;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.utils.TransformationUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record CircleBlockDisplayCue(Material material, int count, int duration, int teleportDuration, float scale) implements FXCue {

    private static final Map<LivingEntity, BukkitRunnable> displays = new HashMap<>();
    private static final Map<LivingEntity, List<BlockDisplay>> activeDisplays = new HashMap<>();

    // A bunch of block displays rotating around the player in a circle
    @Override
    public void run(LivingEntity trigger) {
        if (displays.containsKey(trigger)) {
            // Task is already running for this entity, do not schedule another one
            return;
        }
        double angleIncrement = 2 * Math.PI / count;
        List<BlockDisplay> displaysToAdd = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            BlockDisplay display = trigger.getWorld().spawn(trigger.getLocation(), BlockDisplay.class, blockDisplay -> {
                blockDisplay.setBlock(material.createBlockData());
                blockDisplay.setPersistent(false);
                blockDisplay.setRotation(0, 0);
                blockDisplay.setTeleportDuration(teleportDuration);
                // display.setTransformation(TransformationUtil.scale(display.getTransformation(), scale)); - API is broken
            });
            displaysToAdd.add(display);
        }
        activeDisplays.put(trigger, displaysToAdd);
        BukkitRunnable task = new BukkitRunnable() {
            private int currentTick = 0;
            @Override
            public void run() {
                double currentAngle = currentTick * Math.PI / 10;
                List<BlockDisplay> displays = activeDisplays.get(trigger);
                if (displays == null) {
                    cancel();
                    return;
                }
                for (int i = 0; i < displays.size(); i++) {
                    BlockDisplay display = displays.get(i);
                    double angle = currentAngle + i * angleIncrement;
                    Vector newVector = new Vector(Math.cos(angle) * 2, 0, Math.sin(angle) * 2);
                    if (!display.isValid()) {
                        cancel();
                        return;
                    }
                    Location triggerWithoutRotation = trigger.getLocation().clone().add(0, 1,0);
                    triggerWithoutRotation.setPitch(0);
                    triggerWithoutRotation.setYaw(0);
                    display.teleport(triggerWithoutRotation.add(newVector).toLocation(trigger.getWorld()));
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
        for (BlockDisplay display : activeDisplays.get(trigger)) {
            display.remove();
        }
        activeDisplays.remove(trigger);
        displays.get(trigger).cancel();
        displays.remove(trigger);
    }
}
