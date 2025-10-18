package de.erethon.spellbook.animation;

/**
 * Represents a callback to be executed at a specific tick in an animation
 */
public record AnimationCallback(int tick, Runnable action) {

    public void execute() {
        if (action != null) {
            action.run();
        }
    }
}