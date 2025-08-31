package de.erethon.spellbook.aoe;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellbookSpell;
import de.erethon.spellbook.aoe.visual.AoEDisplayManager;
import de.erethon.spellbook.aoe.visual.AoEBlockChangeManager;
import de.erethon.spellbook.aoe.visual.AoEDisplayBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Display;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Represents an active AoE in the world
 */
public class AoE {
    private final UUID id;
    private final SpellbookSpell sourceSpell;
    private final LivingEntity caster;
    private final Location center;
    private final AoEShape shape;
    private final AoEParameters parameters;
    private final Vector rotation;
    private final Set<LivingEntity> entitiesInside;
    private final long creationTime;
    private final long duration;

    private LivingEntity followTarget;
    private Vector followOffset;

    private AoEEnterCallback enterCallback;
    private AoELeaveCallback leaveCallback;
    private AoETickCallback tickCallback;

    private final Particle.DustOptions friendlyDust;
    private final Particle.DustOptions neutralDust;
    private final Particle.DustOptions enemyDust;

    private final AoEDisplayManager displayManager;
    private final AoEBlockChangeManager blockChangeManager;
    private Material currentBlockMaterial;

    public AoE(SpellbookSpell sourceSpell, LivingEntity caster, Location center,
               AoEShape shape, AoEParameters parameters, Vector rotation, long duration) {
        this.id = UUID.randomUUID();
        this.sourceSpell = sourceSpell;
        this.caster = caster;
        this.center = center.clone();
        this.shape = shape;
        this.parameters = parameters;
        this.rotation = rotation != null ? rotation.clone() : null;
        this.entitiesInside = new HashSet<>();
        this.creationTime = getCurrentTick();
        this.duration = duration;

        this.friendlyDust = new Particle.DustOptions(Color.WHITE, 1.0f);
        this.neutralDust = new Particle.DustOptions(Color.GRAY, 1.0f);
        this.enemyDust = new Particle.DustOptions(Color.RED, 1.0f);

        // Initialize visual enhancement managers
        this.displayManager = new AoEDisplayManager();
        this.blockChangeManager = new AoEBlockChangeManager();

        if (sourceSpell instanceof AoEEnterCallback) {
            this.enterCallback = (AoEEnterCallback) sourceSpell;
        }
        if (sourceSpell instanceof AoELeaveCallback) {
            this.leaveCallback = (AoELeaveCallback) sourceSpell;
        }
        if (sourceSpell instanceof AoETickCallback) {
            this.tickCallback = (AoETickCallback) sourceSpell;
        }
        Spellbook.getInstance().getAoEManager().addAoE(this);
    }

    /**
     * Creates a Display builder for this AoE's center location
     */
    public AoEDisplayBuilder createDisplay() {
        return new AoEDisplayBuilder(center);
    }

    /**
     * Adds a Display entity to this AoE
     */
    public AoE addDisplay(Display display) {
        displayManager.addDisplay(display);
        return this;
    }

    /**
     * Removes a Display entity from this AoE
     */
    public AoE removeDisplay(Display display) {
        displayManager.removeDisplay(display);
        return this;
    }

    /**
     * Gets all Display entities associated with this AoE
     */
    public Set<Display> getDisplays() {
        return displayManager.getDisplays();
    }

    /**
     * Adds block changes that will be sent to nearby players (ground level only)
     */
    public AoE addBlockChange(Material material) {
        Set<Player> nearbyPlayers = getNearbyPlayers();
        this.currentBlockMaterial = material;
        Set<Location> groundBlocks = getGroundBlocksInAoE();
        blockChangeManager.replaceBlocks(nearbyPlayers, groundBlocks, material);
        return this;
    }

    /**
     * Adds block changes with custom BlockData (ground level only)
     */
    public AoE addBlockChange(BlockData blockData) {
        Set<Player> nearbyPlayers = getNearbyPlayers();
        Set<Location> groundBlocks = getGroundBlocksInAoE();
        for (Location loc : groundBlocks) {
            blockChangeManager.addBlockChange(nearbyPlayers, loc, blockData);
        }
        return this;
    }

    /**
     * Adds block changes with random material selection (ground level only)
     */
    public AoE addBlockChange(Material... materials) {
        Set<Player> nearbyPlayers = getNearbyPlayers();
        Set<Location> groundBlocks = getGroundBlocksInAoE();
        blockChangeManager.replaceBlocks(nearbyPlayers, groundBlocks, materials);
        return this;
    }

    /**
     * Adds block changes that will affect the entire AoE volume (3D)
     */
    public AoE addBlockChangeVolume(Material material) {
        Set<Player> nearbyPlayers = getNearbyPlayers();
        Set<Location> areaBlocks = getBlocksInAoE();
        blockChangeManager.replaceBlocks(nearbyPlayers, areaBlocks, material);
        return this;
    }

    /**
     * Adds block changes with custom BlockData that will affect the entire AoE volume (3D)
     */
    public AoE addBlockChangeVolume(BlockData blockData) {
        Set<Player> nearbyPlayers = getNearbyPlayers();
        Set<Location> areaBlocks = getBlocksInAoE();
        for (Location loc : areaBlocks) {
            blockChangeManager.addBlockChange(nearbyPlayers, loc, blockData);
        }
        return this;
    }

    /**
     * Adds blocks on top of solid blocks in the AoE
     */
    public AoE addBlocksOnTop(Material material) {
        return addBlocksOnTop(material, 1);
    }

    /**
     * Adds blocks on top of solid blocks in the AoE with configurable layer thickness
     */
    public AoE addBlocksOnTop(Material material, int layerThickness) {
        Set<Player> nearbyPlayers = getNearbyPlayers();
        Set<Location> areaBlocks = getBlocksInAoE();
        blockChangeManager.addBlockChangesOnTop(nearbyPlayers, areaBlocks, material, layerThickness);
        return this;
    }

    /**
     * Adds block changes that will be sent to nearby players (ground level only) with random material selection
     */
    public AoE addBlocksOnTopGroundLevel(Material... materials) {
        return addBlocksOnTopGroundLevel(1, materials);
    }

    /**
     * Adds block changes that will be sent to nearby players (ground level only) with random material selection and configurable layer thickness
     */
    public AoE addBlocksOnTopGroundLevel(int layerThickness, Material... materials) {
        Set<Player> nearbyPlayers = getNearbyPlayers();
        Set<Location> groundBlocks = getGroundBlocksInAoE();
        blockChangeManager.addBlockChangesOnTop(nearbyPlayers, groundBlocks, layerThickness, materials);
        return this;
    }

    /**
     * Adds block changes with random material selection that will affect the entire AoE volume (3D)
     */
    public AoE addBlockChangeVolume(Material... materials) {
        Set<Player> nearbyPlayers = getNearbyPlayers();
        Set<Location> areaBlocks = getBlocksInAoE();
        blockChangeManager.replaceBlocks(nearbyPlayers, areaBlocks, materials);
        return this;
    }

    /**
     * Adds blocks on top of solid blocks in the AoE with random material selection
     */
    public AoE addBlocksOnTop(Material... materials) {
        return addBlocksOnTop(1, materials);
    }

    /**
     * Adds blocks on top of solid blocks in the AoE with random material selection and configurable layer thickness
     */
    public AoE addBlocksOnTop(int layerThickness, Material... materials) {
        Set<Player> nearbyPlayers = getNearbyPlayers();
        Set<Location> areaBlocks = getBlocksInAoE();
        blockChangeManager.addBlockChangesOnTop(nearbyPlayers, areaBlocks, layerThickness, materials);
        return this;
    }

    /**
     * Sends all queued block changes to players
     */
    public AoE sendBlockChanges() {
        blockChangeManager.sendBlockChanges();
        return this;
    }

    /**
     * Reverts all block changes to their original state
     */
    public AoE revertBlockChanges() {
        blockChangeManager.revertBlockChanges();
        return this;
    }

    /**
     * Gets all block locations that fall within this AoE
     */
    private Set<Location> getBlocksInAoE() {
        Set<Location> blocks = new HashSet<>();
        double searchRadius = calculateSearchRadius();

        int minX = (int) Math.floor(center.getX() - searchRadius);
        int maxX = (int) Math.ceil(center.getX() + searchRadius);
        int minY = (int) Math.floor(center.getY() - parameters.getHeight() / 2);
        int maxY = (int) Math.ceil(center.getY() + parameters.getHeight() / 2);
        int minZ = (int) Math.floor(center.getZ() - searchRadius);
        int maxZ = (int) Math.ceil(center.getZ() + searchRadius);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Location blockLoc = new Location(center.getWorld(), x, y, z);
                    if (shape.contains(center, blockLoc, parameters, rotation)) {
                        blocks.add(blockLoc);
                    }
                }
            }
        }

        return blocks;
    }

    /**
     * Gets all block locations at ground level that fall within this AoE
     */
    private Set<Location> getGroundBlocksInAoE() {
        Set<Location> groundBlocks = new HashSet<>();
        double searchRadius = calculateSearchRadius();

        int minX = (int) Math.floor(center.getX() - searchRadius);
        int maxX = (int) Math.ceil(center.getX() + searchRadius);
        int minZ = (int) Math.floor(center.getZ() - searchRadius);
        int maxZ = (int) Math.ceil(center.getZ() + searchRadius);

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                // Use center Y for shape checking
                Location shapeCheckLoc = new Location(center.getWorld(), x, center.getY(), z);
                if (shape.contains(center, shapeCheckLoc, parameters, rotation)) {
                    // Find the actual solid ground block near the AoE center's Y level
                    Location groundLoc = findNearbyGroundBlock(x, z);
                    if (groundLoc != null) {
                        groundBlocks.add(groundLoc);
                    }
                }
            }
        }

        return groundBlocks;
    }

    /**
     * Finds a solid ground block near the AoE center's Y level (similar to getTerrainAdjustedLocation)
     */
    private Location findNearbyGroundBlock(int x, int z) {
        // Start checking from the center's Y level, then expand search
        for (int yOffset = 0; yOffset >= -2; yOffset--) { // Check current level, then down
            Location checkLoc = new Location(center.getWorld(), x, center.getY() + yOffset, z);
            if (checkLoc.getBlock().isSolid()) {
                return checkLoc;
            }
        }

        for (int yOffset = 1; yOffset <= 2; yOffset++) { // Check above
            Location checkLoc = new Location(center.getWorld(), x, center.getY() + yOffset, z);
            if (checkLoc.getBlock().isSolid()) {
                return checkLoc;
            }
        }

        // If no solid ground found nearby, return null (no block change at this location)
        return null;
    }

    /**
     * Sets a custom enter callback
     */
    public AoE onEnter(AoEEnterCallback callback) {
        this.enterCallback = callback;
        return this;
    }

    /**
     * Sets a custom leave callback
     */
    public AoE onLeave(AoELeaveCallback callback) {
        this.leaveCallback = callback;
        return this;
    }

    /**
     * Sets a custom tick callback
     */
    public AoE onTick(AoETickCallback callback) {
        this.tickCallback = callback;
        return this;
    }

    /**
     * Makes this AoE follow the specified entity
     */
    public AoE followEntity(LivingEntity entity) {
        this.followTarget = entity;
        this.followOffset = new Vector(0, 0, 0);
        return this;
    }

    /**
     * Makes this AoE follow the specified entity with an offset
     */
    public AoE followEntity(LivingEntity entity, Vector offset) {
        this.followTarget = entity;
        this.followOffset = offset != null ? offset.clone() : new Vector(0, 0, 0);
        return this;
    }

    /**
     * Stops following any entity
     */
    public AoE stopFollowing() {
        this.followTarget = null;
        this.followOffset = null;
        return this;
    }

    /**
     * Checks if an entity is within this AoE
     */
    public boolean contains(LivingEntity entity) {
        return shape.contains(center, entity.getLocation(), parameters, rotation);
    }

    /**
     * Called when an entity enters this AoE
     */
    public void onEntityEnter(LivingEntity entity) {
        if (entitiesInside.add(entity)) {
            if (enterCallback != null) {
                enterCallback.onEntityEnter(this, entity);
            }
        }
    }

    /**
     * Called when an entity leaves this AoE
     */
    public void onEntityLeave(LivingEntity entity) {
        if (entitiesInside.remove(entity)) {
            if (leaveCallback != null) {
                leaveCallback.onEntityLeave(this, entity);
            }
        }
    }

    /**
     * Called every tick for spell-specific logic
     */
    public void onTick() {
        Location oldCenter = center.clone();

        if (followTarget != null && !followTarget.isDead()) {
            Location newCenter = followTarget.getLocation().add(followOffset);
            center.setX(newCenter.getX());
            center.setY(newCenter.getY());
            center.setZ(newCenter.getZ());
            center.setWorld(newCenter.getWorld());

            if (!oldCenter.equals(center) && currentBlockMaterial != null) {
                Set<Location> newGroundBlocks = getGroundBlocksInAoE();
                blockChangeManager.updatePosition(oldCenter, center, newGroundBlocks, currentBlockMaterial);
            }
        }

        displayManager.updateDisplayPositions(center);

        if (tickCallback != null) {
            tickCallback.onAoETick(this);
        }
    }

    /**
     * Checks if this AoE has expired
     */
    public boolean isExpired() {
        if (duration < 0) return false; // Permanent
        return getCurrentTick() - creationTime >= duration;
    }

    /**
     * Displays particles for this AoE to nearby players
     */
    public void displayParticles() {
        Set<Player> nearbyPlayers = getNearbyPlayers();

        for (Player player : nearbyPlayers) {
            Particle.DustOptions dust = getDustForPlayer(player);
            spawnShapeParticles(player, dust);
        }
    }

    public void end() {
        Spellbook.getInstance().getAoEManager().removeAoE(id);
    }
    /**
     * Called when the AoE is being removed
     */
    public void cleanup() {
        // Clean up visual enhancements
        displayManager.cleanup();
        blockChangeManager.cleanup();

        // Notify all entities inside that they're leaving
        for (LivingEntity entity : new HashSet<>(entitiesInside)) {
            onEntityLeave(entity);
        }
        entitiesInside.clear();
    }

    /**
     * Gets chunk keys for all chunks this AoE might overlap
     */
    public Set<Long> getOverlappingChunkKeys() {
        Set<Long> chunkKeys = new HashSet<>();
        double searchRadius = calculateSearchRadius();

        int minChunkX = (int) Math.floor((center.getX() - searchRadius) / 16);
        int maxChunkX = (int) Math.floor((center.getX() + searchRadius) / 16);
        int minChunkZ = (int) Math.floor((center.getZ() - searchRadius) / 16);
        int maxChunkZ = (int) Math.floor((center.getZ() + searchRadius) / 16);

        for (int x = minChunkX; x <= maxChunkX; x++) {
            for (int z = minChunkZ; z <= maxChunkZ; z++) {
                chunkKeys.add(getChunkKey(x, z));
            }
        }

        return chunkKeys;
    }

    public static long getChunkKey(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    private Set<Player> getNearbyPlayers() {
        double searchRadius = calculateSearchRadius() + 20;
        Set<Player> players = new HashSet<>();
        for (LivingEntity entity : center.getNearbyLivingEntities(searchRadius)) {
            if (entity instanceof Player) {
                players.add((Player) entity);
            }
        }
        return players;
    }

    private double calculateSearchRadius() {
        switch (shape) {
            case CIRCLE, CONE:
                return parameters.getRadius() + 5;
            case RECTANGLE:
                return Math.max(parameters.getWidth(), parameters.getLength()) + 5;
            default:
                return 10;
        }
    }

    private Particle.DustOptions getDustForPlayer(Player player) {
        if (player.equals(caster)) {
            return friendlyDust;
        }

        if (Spellbook.canAttack(caster, player)) {
            return enemyDust;
        }

        return neutralDust;
    }

    private void spawnShapeParticles(Player player, Particle.DustOptions dust) {
        switch (shape) {
            case CIRCLE:
                spawnCircleParticles(player, dust);
                break;
            case RECTANGLE:
                spawnRectangleParticles(player, dust);
                break;
            case CONE:
                spawnConeParticles(player, dust);
                break;
        }
    }

    private void spawnCircleParticles(Player player, Particle.DustOptions dust) {
        double radius = parameters.getRadius();
        int points = Math.max(8, (int) (radius * 4));

        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            double x = center.getX() + radius * Math.cos(angle);
            double z = center.getZ() + radius * Math.sin(angle);

            Location particleLoc = getTerrainAdjustedLocation(x, center.getY(), z);

            player.spawnParticle(Particle.DUST, particleLoc, 1, 0.1, 0.1, 0.1, 0, dust);
        }
    }

    private void spawnRectangleParticles(Player player, Particle.DustOptions dust) {
        double halfWidth = parameters.getWidth() / 2;
        double halfLength = parameters.getLength() / 2;

        Vector[] corners = {
            new Vector(-halfWidth, 0, -halfLength),
            new Vector(halfWidth, 0, -halfLength),
            new Vector(halfWidth, 0, halfLength),
            new Vector(-halfWidth, 0, halfLength)
        };

        if (rotation != null && rotation.lengthSquared() > 0) {
            double yaw = Math.atan2(-rotation.getX(), rotation.getZ());
            for (Vector corner : corners) {
                rotateVector(corner, yaw);
            }
        }

        for (int i = 0; i < 4; i++) {
            Vector start = corners[i];
            Vector end = corners[(i + 1) % 4];
            drawTerrainFollowingLine(player, dust, start, end);
        }
    }

    private void spawnConeParticles(Player player, Particle.DustOptions dust) {
        Vector direction = rotation != null ? rotation.clone().normalize() : new Vector(0, 0, 1);
        double halfAngle = Math.toRadians(parameters.getAngle() / 2);
        double radius = parameters.getRadius();

        int arcPoints = Math.max(4, (int) (parameters.getAngle() / 8));
        for (int i = 0; i <= arcPoints; i++) {
            double angle = -halfAngle + (2 * halfAngle * i / arcPoints);
            Vector arcDirection = rotateVectorY(direction, angle);
            Vector arcPoint = arcDirection.clone().multiply(radius);

            double x = center.getX() + arcPoint.getX();
            double z = center.getZ() + arcPoint.getZ();
            Location particleLoc = getTerrainAdjustedLocation(x, center.getY(), z);

            player.spawnParticle(Particle.DUST, particleLoc, 1, 0.1, 0.1, 0.1, 0, dust);
        }

        Vector leftEnd = rotateVectorY(direction, -halfAngle).multiply(radius);
        Vector rightEnd = rotateVectorY(direction, halfAngle).multiply(radius);

        drawTerrainFollowingLine(player, dust, new Vector(0, 0, 0), leftEnd);
        drawTerrainFollowingLine(player, dust, new Vector(0, 0, 0), rightEnd);
    }

    /**
     * Draws a line that follows terrain contours
     */
    private void drawTerrainFollowingLine(Player player, Particle.DustOptions dust, Vector start, Vector end) {
        Vector direction = end.clone().subtract(start);
        double distance = direction.length();
        direction.normalize();

        int steps = Math.max(2, (int) (distance * 2)); // Reduced from distance*8 to distance*2
        for (int i = 0; i <= steps; i++) {
            Vector point = start.clone().add(direction.clone().multiply(distance * i / steps));
            double x = center.getX() + point.getX();
            double z = center.getZ() + point.getZ();

            Location particleLoc = getTerrainAdjustedLocation(x, center.getY(), z);
            player.spawnParticle(Particle.DUST, particleLoc, 1, 0.05, 0.05, 0.05, 0, dust);
        }
    }

    /**
     * Gets a location adjusted to follow terrain contours within reasonable bounds
     */
    private Location getTerrainAdjustedLocation(double x, double baseY, double z) {
        Location testLoc = new Location(center.getWorld(), x, baseY, z);

        for (int yOffset = 0; yOffset >= -2; yOffset--) { // Check current level, then down
            Location checkLoc = testLoc.clone().add(0, yOffset, 0);
            if (checkLoc.getBlock().isSolid()) {
                return checkLoc.add(0, 1.1, 0);
            }
        }

        for (int yOffset = 1; yOffset <= 2; yOffset++) {
            Location checkLoc = testLoc.clone().add(0, yOffset, 0);
            if (checkLoc.getBlock().isSolid()) {
                return checkLoc.add(0, 1.1, 0);
            }
        }

        // If no solid ground found nearby, use base Y + small offset
        return testLoc.add(0, 0.1, 0);
    }

    private void rotateVector(Vector vector, double yawRadians) {
        double cos = Math.cos(yawRadians);
        double sin = Math.sin(yawRadians);

        double newX = vector.getX() * cos - vector.getZ() * sin;
        double newZ = vector.getX() * sin + vector.getZ() * cos;

        vector.setX(newX);
        vector.setZ(newZ);
    }

    private Vector rotateVectorY(Vector vector, double yawRadians) {
        double cos = Math.cos(yawRadians);
        double sin = Math.sin(yawRadians);

        double newX = vector.getX() * cos - vector.getZ() * sin;
        double newZ = vector.getX() * sin + vector.getZ() * cos;

        return new Vector(newX, vector.getY(), newZ);
    }

    /**
     * Gets the current server tick count
     */
    private long getCurrentTick() {
        return Bukkit.getCurrentTick();
    }

    // Getters
    public UUID getId() { return id; }
    public SpellbookSpell getSourceSpell() { return sourceSpell; }
    public LivingEntity getCaster() { return caster; }
    public Location getCenter() { return center.clone(); }
    public AoEShape getShape() { return shape; }
    public AoEParameters getParameters() { return parameters; }
    public Set<LivingEntity> getEntitiesInside() { return new HashSet<>(entitiesInside); }
    public long getCreationTime() { return creationTime; }
    public long getDuration() { return duration; }
}
