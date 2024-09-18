package de.erethon.spellbook.fx.cues;

import org.bukkit.entity.LivingEntity;

public interface FXCue {

    void run(LivingEntity trigger);

    default boolean isOnceAndDone() {
        return true;
    }

    default int getDelay() {
        return 0;
    }

    default void onRemove(LivingEntity trigger) {
        // Do nothing
    }

}
