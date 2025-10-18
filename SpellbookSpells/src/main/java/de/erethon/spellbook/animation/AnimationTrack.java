package de.erethon.spellbook.animation;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single display entity's animation over time
 */
public class AnimationTrack {

    private final String id;
    private final List<AnimationKeyframe> keyframes = new ArrayList<>();
    private final DisplayType displayType;

    // Display entity properties
    private Material blockMaterial;
    private ItemStack itemStack;
    private int glowColor = -1;
    private boolean glowing = false;
    private float viewRange = 1.0f;
    private int interpolationDuration = 2; // Default 2 ticks for smooth transitions

    private Display display;
    private Location spawnLocation;

    AnimationTrack(String id, DisplayType displayType) {
        this.id = id;
        this.displayType = displayType;
    }

    /**
     * Spawns the display entity at the origin
     */
    void spawn(Location origin, LivingEntity mountEntity) {
        if (display != null && !display.isDead()) {
            return; // Already spawned
        }

        this.spawnLocation = origin.clone();
        switch (displayType) {
            case BLOCK -> {
                BlockDisplay blockDisplay = (BlockDisplay) origin.getWorld().spawnEntity(origin, EntityType.BLOCK_DISPLAY);
                blockDisplay.setBlock(blockMaterial.createBlockData());
                display = blockDisplay;
            }
            case ITEM -> {
                ItemDisplay itemDisplay = (ItemDisplay) origin.getWorld().spawnEntity(origin, EntityType.ITEM_DISPLAY);
                itemDisplay.setItemStack(itemStack);
                display = itemDisplay;
            }
        }

        if (display != null) {
            display.setPersistent(false);
            display.setViewRange(viewRange);
            // Don't set interpolation here - let applyKeyframe handle it

            if (glowing && glowColor != -1) {
                display.setGlowing(true);
                display.setGlowColorOverride(Color.fromRGB(glowColor));
            }

            if (mountEntity != null && !mountEntity.isDead()) {
                mountEntity.addPassenger(display);
            }

            // Apply first keyframe immediately without interpolation
            if (!keyframes.isEmpty()) {
                AnimationKeyframe firstFrame = keyframes.getFirst();
                if (firstFrame.getTick() == 0) {
                    // Set interpolation to 0 for instant application of first frame
                    display.setInterpolationDelay(0);
                    display.setInterpolationDuration(0);

                    Quaternionf rotation = eulerToQuaternion(
                        firstFrame.getRotationX(),
                        firstFrame.getRotationY(),
                        firstFrame.getRotationZ()
                    );

                    Transformation transformation = new Transformation(
                        firstFrame.getTranslation(),
                        new AxisAngle4f(rotation.angle(), rotation.x(), rotation.y(), rotation.z()),
                        firstFrame.getScale(),
                        new AxisAngle4f(0, 0, 1, 0)
                    );

                    display.setTransformation(transformation);
                }
            }
        }
    }

    /**
     * Updates the display entity based on current tick
     */
    void update(int currentTick, Location origin) {
        if (display == null || display.isDead()) {
            return;
        }
        this.spawnLocation = origin.clone();
        for (AnimationKeyframe keyframe : keyframes) {
            int targetTick = keyframe.getTick();
            if (currentTick == targetTick) {
                applyKeyframe(keyframe);
            }
        }
    }

    /**
     * Applies a keyframe transformation to the display entity using only transformations
     */
    private void applyKeyframe(AnimationKeyframe keyframe) {
        if (display == null || display.isDead()) {
            return;
        }

        display.setInterpolationDelay(-1);
        display.setInterpolationDuration(interpolationDuration);

        Quaternionf rotation = eulerToQuaternion(
            keyframe.getRotationX(),
            keyframe.getRotationY(),
            keyframe.getRotationZ()
        );

        Transformation transformation = new Transformation(
            keyframe.getTranslation(), // Use translation in the transformation
            new AxisAngle4f(rotation.angle(), rotation.x(), rotation.y(), rotation.z()),
            keyframe.getScale(),
            new AxisAngle4f(0, 0, 1, 0) // No left rotation
        );

        display.setTransformation(transformation);
    }

    /**
     * Converts euler angles (in degrees) to quaternion
     */
    private Quaternionf eulerToQuaternion(float pitch, float yaw, float roll) {
        // Convert degrees to radians
        float pitchRad = (float) Math.toRadians(pitch);
        float yawRad = (float) Math.toRadians(yaw);
        float rollRad = (float) Math.toRadians(roll);

        Quaternionf q = new Quaternionf();
        q.rotateXYZ(pitchRad, yawRad, rollRad);
        return q;
    }

    /**
     * Resets the track for looping
     */
    void reset() {
        if (display != null && !keyframes.isEmpty()) {
            AnimationKeyframe firstFrame = keyframes.getFirst();
            if (firstFrame.getTick() == 0) {
                applyKeyframe(firstFrame);
            }
        }
    }

    /**
     * Cleans up the display entity
     */
    void cleanup() {
        if (display != null) {
            display.remove();
        }
        display = null;
    }

    // Builder methods

    void addKeyframe(AnimationKeyframe keyframe) {
        keyframes.add(keyframe);
        keyframes.sort((a, b) -> Integer.compare(a.getTick(), b.getTick()));
    }

    void setBlockMaterial(Material material) {
        this.blockMaterial = material;
    }

    void setItemStack(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    void setGlowColor(int color) {
        this.glowColor = color;
        this.glowing = true;
    }

    void setViewRange(float viewRange) {
        this.viewRange = viewRange;
    }

    void setInterpolationDuration(int ticks) {
        this.interpolationDuration = ticks;
    }

    public String getId() {
        return id;
    }

    public Display getDisplay() {
        return display;
    }

    enum DisplayType {
        BLOCK,
        ITEM
    }
}
