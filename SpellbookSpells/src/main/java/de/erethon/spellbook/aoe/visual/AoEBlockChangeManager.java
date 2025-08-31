package de.erethon.spellbook.aoe.visual;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Manages block changes for AoE visual effects using packet-based updates
 */
public class AoEBlockChangeManager {
    private final Map<Player, Set<BlockState>> playerBlockChanges = new HashMap<>();
    private final Set<Location> affectedBlocks = new HashSet<>();

    /**
     * Adds a block change for specific players
     */
    public void addBlockChange(Collection<Player> players, Location location, Material material) {
        addBlockChange(players, location, material.createBlockData());
    }

    /**
     * Adds a block change with custom BlockData for specific players
     */
    public void addBlockChange(Collection<Player> players, Location location, BlockData blockData) {
        Block block = location.getBlock();
        BlockState newState = block.getState();
        newState.setBlockData(blockData);

        for (Player player : players) {
            playerBlockChanges.computeIfAbsent(player, k -> new HashSet<>()).add(newState);
        }

        affectedBlocks.add(location);
    }

    /**
     * Adds block changes on top of solid blocks in the given locations
     */
    public void addBlockChangesOnTop(Collection<Player> players, Collection<Location> locations, Material material) {
        addBlockChangesOnTop(players, locations, material, 1);
    }

    /**
     * Adds block changes on top of solid blocks with configurable layer thickness
     */
    public void addBlockChangesOnTop(Collection<Player> players, Collection<Location> locations, Material material, int layerThickness) {
        for (Location loc : locations) {
            Block block = loc.getBlock();
            if (block.isSolid()) {
                for (int layer = 1; layer <= layerThickness; layer++) {
                    Location topLocation = loc.clone().add(0, layer, 0);
                    Block topBlock = topLocation.getBlock();
                    if (topBlock.getType() == Material.AIR) {
                        addBlockChange(players, topLocation, material);
                    } else {
                        break;
                    }
                }
            }
        }
    }

    /**
     * Adds block changes on top of solid blocks at ground level only (ignores AoE height)
     */
    public void addBlockChangesOnTopGroundLevel(Collection<Player> players, Collection<Location> centerLocations, Material material, int radius) {
        addBlockChangesOnTopGroundLevel(players, centerLocations, material, radius, 1);
    }

    /**
     * Adds block changes on top of solid blocks at ground level with configurable layer thickness
     */
    public void addBlockChangesOnTopGroundLevel(Collection<Player> players, Collection<Location> centerLocations, Material material, int radius, int layerThickness) {
        Set<Location> groundLevelBlocks = new HashSet<>();

        for (Location center : centerLocations) {
            // Only check blocks in a horizontal circle, ignoring AoE height
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x * x + z * z <= radius * radius) { // Circle check
                        Location blockLoc = center.clone().add(x, 0, z);
                        groundLevelBlocks.add(blockLoc);
                    }
                }
            }
        }

        addBlockChangesOnTop(players, groundLevelBlocks, material, layerThickness);
    }

    /**
     * Replaces blocks at given locations
     */
    public void replaceBlocks(Collection<Player> players, Collection<Location> locations, Material material) {
        for (Location loc : locations) {
            addBlockChange(players, loc, material);
        }
    }

    /**
     * Sends all block changes to their respective players
     */
    public void sendBlockChanges() {
        for (Map.Entry<Player, Set<BlockState>> entry : playerBlockChanges.entrySet()) {
            Player player = entry.getKey();
            Set<BlockState> blockStates = entry.getValue();

            if (player.isOnline() && !blockStates.isEmpty()) {
                player.sendBlockChanges(blockStates);
            }
        }
    }

    /**
     * Reverts all block changes to their original state
     */
    public void revertBlockChanges() {
        for (Map.Entry<Player, Set<BlockState>> entry : playerBlockChanges.entrySet()) {
            Player player = entry.getKey();

            if (player.isOnline()) {
                Set<BlockState> originalStates = new HashSet<>();

                for (Location loc : affectedBlocks) {
                    Block block = loc.getBlock();
                    originalStates.add(block.getState());
                }

                if (!originalStates.isEmpty()) {
                    player.sendBlockChanges(originalStates);
                }
            }
        }
    }

    /**
     * Gets all affected block locations
     */
    public Set<Location> getAffectedBlocks() {
        return new HashSet<>(affectedBlocks);
    }

    /**
     * Gets players who have block changes
     */
    public Set<Player> getAffectedPlayers() {
        return new HashSet<>(playerBlockChanges.keySet());
    }

    /**
     * Removes a player from receiving block changes
     */
    public void removePlayer(Player player) {
        playerBlockChanges.remove(player);
    }

    /**
     * Clears all block changes without reverting
     */
    public void clear() {
        playerBlockChanges.clear();
        affectedBlocks.clear();
    }

    /**
     * Cleanup - reverts changes and clears data
     */
    public void cleanup() {
        revertBlockChanges();
        clear();
    }

    /**
     * Updates block changes when an AoE moves position
     */
    public void updatePosition(Location oldCenter, Location newCenter, Set<Location> newGroundBlocks, Material blockMaterial) {
        Set<Location> currentBlocks = new HashSet<>(affectedBlocks);
        Set<Location> blocksToRemove = new HashSet<>(currentBlocks);
        blocksToRemove.removeAll(newGroundBlocks);

        Set<Location> blocksToAdd = new HashSet<>(newGroundBlocks);
        blocksToAdd.removeAll(currentBlocks);

        Set<Player> nearbyPlayers = new HashSet<>();
        for (LivingEntity entity : newCenter.getNearbyLivingEntities(50)) {
            if (entity instanceof Player) {
                nearbyPlayers.add((Player) entity);
            }
        }

        if (!blocksToRemove.isEmpty()) {
            for (Map.Entry<Player, Set<BlockState>> entry : playerBlockChanges.entrySet()) {
                Player player = entry.getKey();
                if (player.isOnline()) {
                    Set<BlockState> originalStates = new HashSet<>();
                    for (Location loc : blocksToRemove) {
                        Block block = loc.getBlock();
                        originalStates.add(block.getState());
                    }
                    if (!originalStates.isEmpty()) {
                        player.sendBlockChanges(originalStates);
                    }
                }
            }

            for (Player player : playerBlockChanges.keySet()) {
                Set<BlockState> playerStates = playerBlockChanges.get(player);
                playerStates.removeIf(state -> blocksToRemove.contains(state.getLocation()));
            }
            affectedBlocks.removeAll(blocksToRemove);
        }

        if (!blocksToAdd.isEmpty()) {
            replaceBlocks(nearbyPlayers, blocksToAdd, blockMaterial);
            sendBlockChanges();
        }
    }
}
