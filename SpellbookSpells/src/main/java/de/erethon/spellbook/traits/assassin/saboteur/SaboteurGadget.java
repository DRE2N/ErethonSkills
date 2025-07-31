package de.erethon.spellbook.traits.assassin.saboteur;

import org.bukkit.entity.BlockDisplay;

public record SaboteurGadget(BlockDisplay display, String id, long spawnTime, int duration, SaboteurGadgetType type) {

    public boolean isExpired() {
        return System.currentTimeMillis() - spawnTime > duration;
    }

    public void remove() {
        display.remove();
    }
}
