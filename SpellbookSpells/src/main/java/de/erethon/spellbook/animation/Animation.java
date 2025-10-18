package de.erethon.spellbook.animation;

import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import java.util.*;

/**
 * Represents a complete animation sequence with multiple tracks
 */
public class Animation {

    private final Map<String, AnimationTrack> tracks = new HashMap<>();
    private final List<AnimationCallback> callbacks = new ArrayList<>();
    private final int duration; // in ticks
    private final boolean loop;

    private Location origin;
    private LivingEntity followEntity;

    private int currentTick = 0;
    private boolean isPlaying = false;
    private boolean isFinished = false;

    Animation(int duration, boolean loop) {
        this.duration = duration;
        this.loop = loop;
    }

    /**
     * Starts the animation
     */
    public void start() {
        if (isPlaying) {
            return;
        }
        isPlaying = true;
        currentTick = 0;
        isFinished = false;

        // Spawn all display entities
        for (AnimationTrack track : tracks.values()) {
            track.spawn(getEffectiveOrigin(), followEntity);
        }
    }

    /**
     * Stops and cleans up the animation
     */
    public void stop() {
        isPlaying = false;
        isFinished = true;
        cleanup();
    }

    /**
     * Called every tick to update the animation
     */
    public void tick() {
        if (!isPlaying || isFinished) {
            return;
        }

        Location effectiveOrigin = getEffectiveOrigin();

        // Update all tracks BEFORE incrementing tick
        // This allows keyframes to be applied at the correct timing for interpolation
        for (AnimationTrack track : tracks.values()) {
            track.update(currentTick, effectiveOrigin);
        }

        // Execute callbacks for this tick
        for (AnimationCallback callback : callbacks) {
            if (callback.tick() == currentTick) {
                callback.execute();
            }
        }

        // Increment tick after updates
        currentTick++;

        // Check if animation is finished
        if (currentTick >= duration) {
            if (loop) {
                currentTick = 0;
                // Reset all tracks for looping
                for (AnimationTrack track : tracks.values()) {
                    track.reset();
                }
            } else {
                isFinished = true;
                isPlaying = false;
                cleanup();
            }
        }
    }

    /**
     * Cleans up all display entities
     */
    private void cleanup() {
        for (AnimationTrack track : tracks.values()) {
            track.cleanup();
        }
    }

    /**
     * Gets the effective origin location based on follow settings
     */
    private Location getEffectiveOrigin() {
        if (followEntity != null && !followEntity.isDead()) {
            Location loc = followEntity.getLocation();
            // Zero out pitch and yaw so animation orientation is independent of entity's look direction
            loc.setPitch(0);
            loc.setYaw(0);
            return loc;
        }
        if (origin != null) {
            origin.setPitch(0);
            origin.setYaw(0);
            return origin;
        }
        return null;
    }

    // Getters and setters

    void addTrack(String id, AnimationTrack track) {
        tracks.put(id, track);
    }

    void addCallback(AnimationCallback callback) {
        callbacks.add(callback);
    }

    public void setOrigin(Location origin) {
        this.origin = origin.clone();
    }

    public void setFollowEntity(LivingEntity entity) {
        this.followEntity = entity;
    }


    public boolean isPlaying() {
        return isPlaying;
    }

    public boolean isFinished() {
        return isFinished;
    }

    public int getCurrentTick() {
        return currentTick;
    }

    public int getDuration() {
        return duration;
    }

}
