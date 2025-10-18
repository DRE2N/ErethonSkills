package de.erethon.spellbook.animation;

import org.joml.Vector3f;

/**
 * Represents a keyframe in an animation track at a specific time
 */
public class AnimationKeyframe {

    private final int tick;
    private final Vector3f translation; // Relative position from origin
    private final float rotationX; // Pitch in degrees
    private final float rotationY; // Yaw in degrees
    private final float rotationZ; // Roll in degrees
    private final Vector3f scale;

    AnimationKeyframe(int tick, Vector3f translation, float rotationX, float rotationY, float rotationZ, Vector3f scale) {
        this.tick = tick;
        this.translation = translation;
        this.rotationX = rotationX;
        this.rotationY = rotationY;
        this.rotationZ = rotationZ;
        this.scale = scale;
    }

    public int getTick() {
        return tick;
    }

    public Vector3f getTranslation() {
        return new Vector3f(translation);
    }

    public float getRotationX() {
        return rotationX;
    }

    public float getRotationY() {
        return rotationY;
    }

    public float getRotationZ() {
        return rotationZ;
    }

    public Vector3f getScale() {
        return new Vector3f(scale);
    }
}

