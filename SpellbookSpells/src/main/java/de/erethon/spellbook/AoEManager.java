package de.erethon.spellbook;

import de.erethon.spellbook.aoe.AoE;
import io.papermc.paper.event.entity.EntityMoveEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AoEManager implements Listener {

    // Chunk-based AoE lookup - using long as chunk key (x << 32 | z)
    private final Map<Long, Set<AoE>> aoEsByChunk = new ConcurrentHashMap<>();
    private final Map<UUID, AoE> activeAoEs = new ConcurrentHashMap<>();
    private final Map<UUID, Set<Long>> aoeChunkMappings = new ConcurrentHashMap<>(); // AoE ID -> chunk keys

    private BukkitRunnable updateTask;
    private BukkitRunnable particleTask;

    public AoEManager() {
        // Register event listener
        Bukkit.getPluginManager().registerEvents(this, Spellbook.getInstance().getImplementer());

        // Start update tasks
        startUpdateTask();
        startParticleTask();
    }

    /**
     * Registers a new AoE
     */
    public void addAoE(AoE aoe) {
        activeAoEs.put(aoe.getId(), aoe);

        // Add to chunk-based lookup
        Set<Long> chunkKeys = aoe.getOverlappingChunkKeys();
        aoeChunkMappings.put(aoe.getId(), chunkKeys);

        for (Long chunkKey : chunkKeys) {
            aoEsByChunk.computeIfAbsent(chunkKey, k -> ConcurrentHashMap.newKeySet()).add(aoe);
        }
    }

    /**
     * Removes an AoE
     */
    public void removeAoE(UUID aoeId) {
        AoE aoe = activeAoEs.remove(aoeId);
        if (aoe == null) return;

        // Clean up from chunk lookup
        Set<Long> chunkKeys = aoeChunkMappings.remove(aoeId);
        if (chunkKeys != null) {
            for (Long chunkKey : chunkKeys) {
                Set<AoE> chunkAoEs = aoEsByChunk.get(chunkKey);
                if (chunkAoEs != null) {
                    chunkAoEs.remove(aoe);
                    if (chunkAoEs.isEmpty()) {
                        aoEsByChunk.remove(chunkKey);
                    }
                }
            }
        }

        // Call cleanup
        aoe.cleanup();
    }

    /**
     * Gets all AoEs in the same chunks as the given location
     */
    public Set<AoE> getAoEsInChunk(Location location) {
        long chunkKey = AoE.getChunkKey(location.getChunk().getX(), location.getChunk().getZ());
        Set<AoE> chunkAoEs = aoEsByChunk.get(chunkKey);
        return chunkAoEs != null ? new HashSet<>(chunkAoEs) : Collections.emptySet();
    }

    /**
     * Gets all AoEs that might affect an entity at the given location
     */
    public Set<AoE> getNearbyAoEs(Location location) {
        Set<AoE> nearby = new HashSet<>();

        // Check current chunk and adjacent chunks
        int chunkX = location.getChunk().getX();
        int chunkZ = location.getChunk().getZ();

        for (int x = chunkX - 1; x <= chunkX + 1; x++) {
            for (int z = chunkZ - 1; z <= chunkZ + 1; z++) {
                long chunkKey = AoE.getChunkKey(x, z);
                Set<AoE> chunkAoEs = aoEsByChunk.get(chunkKey);
                if (chunkAoEs != null) {
                    nearby.addAll(chunkAoEs);
                }
            }
        }

        return nearby;
    }

    /**
     * Handles player movement for AoE checking
     */
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();

        // Only check if player actually moved to a different block
        if (to == null || (from.getBlockX() == to.getBlockX() &&
                          from.getBlockY() == to.getBlockY() &&
                          from.getBlockZ() == to.getBlockZ())) {
            return;
        }

        checkEntityMovement(event.getPlayer(), from, to);
    }

    /**
     * Handles entity movement for AoE checking
     */
    @EventHandler public void onEntityMove(EntityMoveEvent event) {

        if (!event.hasExplicitlyChangedBlock()) {
            return;
        }
        checkEntityMovement(event.getEntity(), event.getFrom(), event.getTo());
    }

    /**
     * Common logic for checking entity movement between AoEs
     */
    private void checkEntityMovement(LivingEntity entity, Location from, Location to) {
        Set<AoE> oldAoEs = getNearbyAoEs(from);
        Set<AoE> newAoEs = getNearbyAoEs(to);

        Set<AoE> allRelevantAoEs = new HashSet<>(oldAoEs);
        allRelevantAoEs.addAll(newAoEs);

        Set<AoE> currentlyInside = new HashSet<>();
        Set<AoE> wasInside = new HashSet<>();

        for (AoE aoe : allRelevantAoEs) {
            boolean nowInside = aoe.contains(entity);
            boolean wasInsideBefore = aoe.getEntitiesInside().contains(entity);

            if (nowInside) {
                currentlyInside.add(aoe);
                if (!wasInsideBefore) {
                    aoe.onEntityEnter(entity);
                }
            } else if (wasInsideBefore) {
                aoe.onEntityLeave(entity);
            }
        }
    }

    /**
     * Updates all AoEs (checks for expiration and calls tick callbacks)
     */
    private void startUpdateTask() {
        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Map.Entry<UUID, AoE> entry : activeAoEs.entrySet()) {
                    AoE aoe = entry.getValue();

                    if (aoe.isExpired()) {
                        removeAoE(aoe.getId());
                        continue;
                    }

                    aoe.onTick();
                }
            }
        };
        updateTask.runTaskTimer(Spellbook.getInstance().getImplementer(), 1L, 1L); // Run every tick
    }

    /**
     * Displays particles for all AoEs
     */
    private void startParticleTask() {
        particleTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (AoE aoe : activeAoEs.values()) {
                    aoe.displayParticles();
                }
            }
        };
        particleTask.runTaskTimer(Spellbook.getInstance().getImplementer(), 1L, 4L);
    }

    /**
     * Shuts down the manager
     */
    public void shutdown() {
        if (updateTask != null) {
            updateTask.cancel();
        }
        if (particleTask != null) {
            particleTask.cancel();
        }

        for (AoE aoe : new ArrayList<>(activeAoEs.values())) {
            removeAoE(aoe.getId());
        }
    }

    /**
     * Gets utility methods for team checking
     */
    public Set<LivingEntity> getNearbyFriendlies(LivingEntity caster, Location center, double radius) {
        Set<LivingEntity> friendlies = new HashSet<>();

        for (LivingEntity entity : center.getNearbyLivingEntities(radius)) {
            if (entity.equals(caster) || !Spellbook.getInstance().canAttack(caster, entity)) {
                friendlies.add(entity);
            }
        }

        return friendlies;
    }

    public Set<LivingEntity> getNearbyEnemies(LivingEntity caster, Location center, double radius) {
        Set<LivingEntity> enemies = new HashSet<>();

        for (LivingEntity entity : center.getNearbyLivingEntities(radius)) {
            if (!entity.equals(caster) && Spellbook.getInstance().canAttack(caster, entity)) {
                enemies.add(entity);
            }
        }

        return enemies;
    }

    public Collection<AoE> getAllAoEs() {
        return Collections.unmodifiableCollection(activeAoEs.values());
    }

    public AoE getAoE(UUID id) {
        return activeAoEs.get(id);
    }

    public int getActiveAoECount() {
        return activeAoEs.size();
    }

    /**
     * Debug method to get chunk distribution
     */
    public Map<Long, Integer> getChunkDistribution() {
        Map<Long, Integer> distribution = new HashMap<>();
        for (Map.Entry<Long, Set<AoE>> entry : aoEsByChunk.entrySet()) {
            distribution.put(entry.getKey(), entry.getValue().size());
        }
        return distribution;
    }
}
