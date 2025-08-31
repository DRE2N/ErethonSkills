package de.erethon.spellbook.aoe;

import org.bukkit.entity.LivingEntity;

/**
 * Functional interface for handling entity enter events in AoEs
 */
@FunctionalInterface
public interface AoEEnterCallback {

    /**
     * Called when an entity enters an AoE
     * @param aoe The AoE that was entered
     * @param entity The entity that entered
     */
    void onEntityEnter(AoE aoe, LivingEntity entity);
}
