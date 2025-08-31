package de.erethon.spellbook.aoe.visual;

import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Manages Display entities for AoE visual effects
 */
public class AoEDisplayManager {
    private final Set<Display> displays = new HashSet<>();

    /**
     * Adds a Display entity to this AoE
     */
    public void addDisplay(Display display) {
        displays.add(display);
    }

    /**
     * Removes a specific Display entity
     */
    public void removeDisplay(Display display) {
        if (displays.remove(display)) {
            display.remove();
        }
    }

    /**
     * Gets all Display entities
     */
    public Set<Display> getDisplays() {
        return new HashSet<>(displays);
    }

    /**
     * Updates the position of all displays relative to a center location
     */
    public void updateDisplayPositions(Location center) {
        for (Display display : displays) {
            // Keep displays relative to center - can be customized per display type
            if (!display.isDead()) {
                display.teleport(center);
            }
        }
    }

    /**
     * Removes all Display entities
     */
    public void cleanup() {
        for (Display display : displays) {
            if (!display.isDead()) {
                display.remove();
            }
        }
        displays.clear();
    }

    /**
     * Checks if any displays are still valid
     */
    public boolean hasValidDisplays() {
        displays.removeIf(Display::isDead);
        return !displays.isEmpty();
    }

    /**
     * Gets the number of active displays
     */
    public int getDisplayCount() {
        displays.removeIf(Display::isDead);
        return displays.size();
    }
}
