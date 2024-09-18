package de.erethon.spellbook.fx.cues;

import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;

public record MovingBlockDisplayCue(Material mat, Vector vector, int teleportDuration, int duration) implements FXCue {

    private static final Map<LivingEntity, BlockDisplay> displays = new HashMap<>();

    @Override
    public void run(LivingEntity trigger) {
        BlockDisplay display = trigger.getWorld().spawn(trigger.getLocation(), BlockDisplay.class, blockDisplay -> {
            blockDisplay.setBlock(mat.createBlockData());
            blockDisplay.setRotation(0, 0);
            blockDisplay.setTeleportDuration(teleportDuration);
            blockDisplay.setPersistent(false);
        });
        display.teleport(trigger.getLocation().add(vector));
        displays.put(trigger, display);
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
        displays.get(trigger).remove();
        displays.remove(trigger);
    }
}
