package de.erethon.spellbook.aoe;

import org.bukkit.entity.LivingEntity;

/**
 * Functional interface for handling entity leave events in AoEs
 */
@FunctionalInterface
public interface AoELeaveCallback {

    /**
     * Called when an entity leaves an AoE
     * @param aoe The AoE that was left
     * @param entity The entity that left
     */
    void onEntityLeave(AoE aoe, LivingEntity entity);
}
