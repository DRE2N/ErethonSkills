package de.erethon.spellbook.animation;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.joml.Vector3f;

/**
 * Fluent builder for creating animations
 */
public class AnimationBuilder {

    private final Animation animation;
    private TrackBuilder currentTrack;

    private AnimationBuilder(int duration, boolean loop) {
        this.animation = new Animation(duration, loop);
    }

    /**
     * Creates a new animation builder
     * @param duration Duration in ticks
     */
    public static AnimationBuilder create(int duration) {
        return new AnimationBuilder(duration, false);
    }

    /**
     * Creates a new looping animation builder
     * @param duration Duration of one loop in ticks
     */
    public static AnimationBuilder createLooping(int duration) {
        return new AnimationBuilder(duration, true);
    }

    /**
     * Sets the origin location for the animation (fixed position)
     */
    public AnimationBuilder atLocation(Location location) {
        animation.setOrigin(location);
        return this;
    }

    /**
     * Makes the animation follow an entity
     */
    public AnimationBuilder followEntity(LivingEntity entity) {
        animation.setFollowEntity(entity);
        return this;
    }

    /**
     * Adds a new BlockDisplay track
     */
    public TrackBuilder addBlockTrack(String id, Material material) {
        finalizeCurrentTrack();
        AnimationTrack track = new AnimationTrack(id, AnimationTrack.DisplayType.BLOCK);
        track.setBlockMaterial(material);
        animation.addTrack(id, track);
        currentTrack = new TrackBuilder(this, track);
        return currentTrack;
    }

    /**
     * Adds a new ItemDisplay track
     */
    public TrackBuilder addItemTrack(String id, ItemStack item) {
        finalizeCurrentTrack();
        AnimationTrack track = new AnimationTrack(id, AnimationTrack.DisplayType.ITEM);
        track.setItemStack(item);
        animation.addTrack(id, track);
        currentTrack = new TrackBuilder(this, track);
        return currentTrack;
    }

    /**
     * Adds a callback at a specific time
     */
    public AnimationBuilder atTime(int tick, Runnable callback) {
        animation.addCallback(new AnimationCallback(tick, callback));
        return this;
    }

    /**
     * Builds and returns the animation
     */
    public Animation build() {
        finalizeCurrentTrack();
        return animation;
    }

    private void finalizeCurrentTrack() {
        // Nothing to finalize for now, but kept for future use
        currentTrack = null;
    }

    /**
     * Builder for individual animation tracks
     */
    public static class TrackBuilder {
        private final AnimationBuilder parent;
        private final AnimationTrack track;
        private KeyframeBuilder currentKeyframe;

        private TrackBuilder(AnimationBuilder parent, AnimationTrack track) {
            this.parent = parent;
            this.track = track;
        }

        /**
         * Sets the glow color for this track
         */
        public TrackBuilder glow(int rgb) {
            track.setGlowColor(rgb);
            return this;
        }

        /**
         * Sets the view range for this display
         */
        public TrackBuilder viewRange(float range) {
            track.setViewRange(range);
            return this;
        }

        /**
         * Sets the interpolation duration (smoothness between keyframes)
         */
        public TrackBuilder interpolationDuration(int ticks) {
            track.setInterpolationDuration(ticks);
            return this;
        }

        /**
         * Starts defining a keyframe at a specific time
         */
        public KeyframeBuilder at(int tick) {
            finalizeCurrentKeyframe();
            currentKeyframe = new KeyframeBuilder(this, tick);
            return currentKeyframe;
        }

        /**
         * Finishes this track and returns to animation builder
         */
        public AnimationBuilder endTrack() {
            finalizeCurrentKeyframe();
            return parent;
        }

        /**
         * Adds a new track (convenience method)
         */
        public TrackBuilder addBlockTrack(String id, Material material) {
            finalizeCurrentKeyframe();
            return parent.addBlockTrack(id, material);
        }

        /**
         * Adds a new item track (convenience method)
         */
        public TrackBuilder addItemTrack(String id, ItemStack item) {
            finalizeCurrentKeyframe();
            return parent.addItemTrack(id, item);
        }

        /**
         * Adds a callback at a specific time (convenience method)
         */
        public AnimationBuilder atTime(int tick, Runnable callback) {
            finalizeCurrentKeyframe();
            return parent.atTime(tick, callback);
        }

        /**
         * Builds the animation (convenience method)
         */
        public Animation build() {
            finalizeCurrentKeyframe();
            return parent.build();
        }

        private void finalizeCurrentKeyframe() {
            if (currentKeyframe != null) {
                track.addKeyframe(currentKeyframe.buildKeyframe());
                currentKeyframe = null;
            }
        }

        /**
         * Creates a circular motion pattern around the origin
         * @param startTick Starting tick
         * @param endTick Ending tick
         * @param radius Radius of the circle
         * @param height Y offset (height above origin)
         * @param startAngle Starting angle in degrees (0 = positive X axis)
         * @param revolutions Number of full rotations (can be negative for counter-clockwise)
         * @param ticksPerKeyframe How often to create keyframes (lower = smoother, but more keyframes)
         */
        public TrackBuilder circle(int startTick, int endTick, float radius, float height, float startAngle, float revolutions, int ticksPerKeyframe) {
            finalizeCurrentKeyframe();

            int duration = endTick - startTick;
            int numKeyframes = Math.max(2, duration / ticksPerKeyframe);
            float angleIncrement = (float) (revolutions * 360.0 / numKeyframes);

            for (int i = 0; i <= numKeyframes; i++) {
                int tick = startTick + (i * duration / numKeyframes);
                float angle = (float) Math.toRadians(startAngle + (angleIncrement * i));
                float x = (float) Math.cos(angle) * radius;
                float z = (float) Math.sin(angle) * radius;

                currentKeyframe = new KeyframeBuilder(this, tick);
                currentKeyframe.position(x, height, z);
                track.addKeyframe(currentKeyframe.buildKeyframe());
                currentKeyframe = null;
            }

            return this;
        }

        /**
         * Creates a circular motion pattern with default settings (1 full rotation, 2 ticks per keyframe)
         */
        public TrackBuilder circle(int startTick, int endTick, float radius, float height) {
            return circle(startTick, endTick, radius, height, 0, 1.0f, 2);
        }

        /**
         * Creates a spiral motion pattern
         * @param startTick Starting tick
         * @param endTick Ending tick
         * @param startRadius Starting radius
         * @param endRadius Ending radius
         * @param startHeight Starting height
         * @param endHeight Ending height
         * @param startAngle Starting angle in degrees
         * @param revolutions Number of full rotations
         * @param ticksPerKeyframe How often to create keyframes
         */
        public TrackBuilder spiral(int startTick, int endTick, float startRadius, float endRadius,
                                   float startHeight, float endHeight, float startAngle,
                                   float revolutions, int ticksPerKeyframe) {
            finalizeCurrentKeyframe();

            int duration = endTick - startTick;
            int numKeyframes = Math.max(2, duration / ticksPerKeyframe);
            float angleIncrement = (float) (revolutions * 360.0 / numKeyframes);

            for (int i = 0; i <= numKeyframes; i++) {
                int tick = startTick + (i * duration / numKeyframes);
                float progress = (float) i / numKeyframes;
                float angle = (float) Math.toRadians(startAngle + (angleIncrement * i));
                float radius = startRadius + (endRadius - startRadius) * progress;
                float height = startHeight + (endHeight - startHeight) * progress;

                float x = (float) Math.cos(angle) * radius;
                float z = (float) Math.sin(angle) * radius;

                currentKeyframe = new KeyframeBuilder(this, tick);
                currentKeyframe.position(x, height, z);
                track.addKeyframe(currentKeyframe.buildKeyframe());
                currentKeyframe = null;
            }

            return this;
        }

        /**
         * Creates an arc motion pattern (partial circle)
         * @param startTick Starting tick
         * @param endTick Ending tick
         * @param radius Radius of the arc
         * @param height Y offset
         * @param startAngle Starting angle in degrees
         * @param endAngle Ending angle in degrees
         * @param ticksPerKeyframe How often to create keyframes
         */
        public TrackBuilder arc(int startTick, int endTick, float radius, float height,
                                float startAngle, float endAngle, int ticksPerKeyframe) {
            finalizeCurrentKeyframe();

            int duration = endTick - startTick;
            int numKeyframes = Math.max(2, duration / ticksPerKeyframe);
            float angleDelta = endAngle - startAngle;

            for (int i = 0; i <= numKeyframes; i++) {
                int tick = startTick + (i * duration / numKeyframes);
                float progress = (float) i / numKeyframes;
                float angle = (float) Math.toRadians(startAngle + (angleDelta * progress));

                float x = (float) Math.cos(angle) * radius;
                float z = (float) Math.sin(angle) * radius;

                currentKeyframe = new KeyframeBuilder(this, tick);
                currentKeyframe.position(x, height, z);
                track.addKeyframe(currentKeyframe.buildKeyframe());
                currentKeyframe = null;
            }

            return this;
        }

        /**
         * Repeats a sequence of transformations over a time range
         * @param startTick Starting tick
         * @param endTick Ending tick
         * @param repetitions Number of times to repeat the sequence
         * @param sequence Function that defines one repetition cycle (receives TrackBuilder and cycle number)
         */
        public TrackBuilder repeat(int startTick, int endTick, int repetitions, java.util.function.BiConsumer<TrackBuilder, Integer> sequence) {
            finalizeCurrentKeyframe();

            int duration = endTick - startTick;
            int cycleDuration = duration / repetitions;

            for (int i = 0; i < repetitions; i++) {
                int cycleStart = startTick + (i * cycleDuration);
                sequence.accept(this, i);
            }

            return this;
        }

        /**
         * Creates a figure-8 pattern (lemniscate)
         * @param startTick Starting tick
         * @param endTick Ending tick
         * @param radius Size of the figure-8
         * @param height Y offset
         * @param revolutions Number of full figure-8 cycles
         * @param ticksPerKeyframe How often to create keyframes
         */
        public TrackBuilder figure8(int startTick, int endTick, float radius, float height,
                                    float revolutions, int ticksPerKeyframe) {
            finalizeCurrentKeyframe();

            int duration = endTick - startTick;
            int numKeyframes = Math.max(2, duration / ticksPerKeyframe);
            float angleIncrement = (float) (revolutions * 2 * Math.PI / numKeyframes);

            for (int i = 0; i <= numKeyframes; i++) {
                int tick = startTick + (i * duration / numKeyframes);
                float t = angleIncrement * i;

                // Lemniscate parametric equations
                float scale = radius / (1 + (float) Math.pow(Math.sin(t), 2));
                float x = scale * (float) Math.cos(t);
                float z = scale * (float) Math.sin(t) * (float) Math.cos(t);

                currentKeyframe = new KeyframeBuilder(this, tick);
                currentKeyframe.position(x, height, z);
                track.addKeyframe(currentKeyframe.buildKeyframe());
                currentKeyframe = null;
            }

            return this;
        }

        /**
         * Creates a wave pattern along one axis
         * @param startTick Starting tick
         * @param endTick Ending tick
         * @param amplitude Height of the wave
         * @param wavelength Distance for one complete wave
         * @param axis Which axis to move along ("x" or "z")
         * @param baseHeight Y offset
         * @param ticksPerKeyframe How often to create keyframes
         */
        public TrackBuilder wave(int startTick, int endTick, float amplitude, float wavelength,
                                 String axis, float baseHeight, int ticksPerKeyframe) {
            finalizeCurrentKeyframe();

            int duration = endTick - startTick;
            int numKeyframes = Math.max(2, duration / ticksPerKeyframe);

            for (int i = 0; i <= numKeyframes; i++) {
                int tick = startTick + (i * duration / numKeyframes);
                float progress = (float) i / numKeyframes;
                float distance = wavelength * progress;
                float height = baseHeight + amplitude * (float) Math.sin(2 * Math.PI * distance / wavelength);

                currentKeyframe = new KeyframeBuilder(this, tick);
                if (axis.equalsIgnoreCase("x")) {
                    currentKeyframe.position(distance, height, 0);
                } else {
                    currentKeyframe.position(0, height, distance);
                }
                track.addKeyframe(currentKeyframe.buildKeyframe());
                currentKeyframe = null;
            }

            return this;
        }

        /**
         * Creates a straight line motion forward from the origin
         * @param startTick Starting tick
         * @param endTick Ending tick
         * @param distance Total distance to travel
         * @param direction Direction vector (will be normalized). Use Vector3f(1,0,0) for +X, (0,1,0) for +Y, (0,0,1) for +Z
         * @param ticksPerKeyframe How often to create keyframes
         */
        public TrackBuilder line(int startTick, int endTick, float distance, Vector3f direction, int ticksPerKeyframe) {
            finalizeCurrentKeyframe();

            // Normalize direction
            Vector3f normalizedDir = new Vector3f(direction).normalize();

            int duration = endTick - startTick;
            int numKeyframes = Math.max(2, duration / ticksPerKeyframe);

            for (int i = 0; i <= numKeyframes; i++) {
                int tick = startTick + (i * duration / numKeyframes);
                float progress = (float) i / numKeyframes;
                float currentDistance = distance * progress;

                float x = normalizedDir.x * currentDistance;
                float y = normalizedDir.y * currentDistance;
                float z = normalizedDir.z * currentDistance;

                currentKeyframe = new KeyframeBuilder(this, tick);
                currentKeyframe.position(x, y, z);
                track.addKeyframe(currentKeyframe.buildKeyframe());
                currentKeyframe = null;
            }

            return this;
        }

        /**
         * Creates a straight line motion forward (simplified version using distance and angle)
         * @param startTick Starting tick
         * @param endTick Ending tick
         * @param distance Total distance to travel
         * @param yaw Horizontal angle in degrees (0 = +Z, 90 = -X, 180 = -Z, 270 = +X)
         * @param pitch Vertical angle in degrees (0 = horizontal, 90 = straight up, -90 = straight down)
         * @param ticksPerKeyframe How often to create keyframes
         */
        public TrackBuilder line(int startTick, int endTick, float distance, float yaw, float pitch, int ticksPerKeyframe) {
            // Convert angles to direction vector
            float yawRad = (float) Math.toRadians(yaw);
            float pitchRad = (float) Math.toRadians(pitch);

            float x = -(float) Math.sin(yawRad) * (float) Math.cos(pitchRad);
            float y = (float) Math.sin(pitchRad);
            float z = (float) Math.cos(yawRad) * (float) Math.cos(pitchRad);

            return line(startTick, endTick, distance, new Vector3f(x, y, z), ticksPerKeyframe);
        }

        /**
         * Creates a straight line motion from point A to point B
         * @param startTick Starting tick
         * @param endTick Ending tick
         * @param startX Starting X position
         * @param startY Starting Y position
         * @param startZ Starting Z position
         * @param endX Ending X position
         * @param endY Ending Y position
         * @param endZ Ending Z position
         * @param ticksPerKeyframe How often to create keyframes
         */
        public TrackBuilder lineBetween(int startTick, int endTick, float startX, float startY, float startZ,
                                        float endX, float endY, float endZ, int ticksPerKeyframe) {
            finalizeCurrentKeyframe();

            int duration = endTick - startTick;
            int numKeyframes = Math.max(2, duration / ticksPerKeyframe);

            for (int i = 0; i <= numKeyframes; i++) {
                int tick = startTick + (i * duration / numKeyframes);
                float progress = (float) i / numKeyframes;

                float x = startX + (endX - startX) * progress;
                float y = startY + (endY - startY) * progress;
                float z = startZ + (endZ - startZ) * progress;

                currentKeyframe = new KeyframeBuilder(this, tick);
                currentKeyframe.position(x, y, z);
                track.addKeyframe(currentKeyframe.buildKeyframe());
                currentKeyframe = null;
            }

            return this;
        }

        /**
         * Creates a straight line motion from point A to point B (Vector3f version)
         * @param startTick Starting tick
         * @param endTick Ending tick
         * @param start Starting position
         * @param end Ending position
         * @param ticksPerKeyframe How often to create keyframes
         */
        public TrackBuilder lineBetween(int startTick, int endTick, Vector3f start, Vector3f end, int ticksPerKeyframe) {
            return lineBetween(startTick, endTick, start.x, start.y, start.z, end.x, end.y, end.z, ticksPerKeyframe);
        }

        /**
         * Creates a zigzag pattern along an axis
         * @param startTick Starting tick
         * @param endTick Ending tick
         * @param distance Total forward distance
         * @param amplitude Width of zigzag (perpendicular to movement)
         * @param cycles Number of complete zigzag cycles
         * @param forwardAxis Primary movement axis ("x" or "z")
         * @param height Y offset
         * @param ticksPerKeyframe How often to create keyframes
         */
        public TrackBuilder zigzag(int startTick, int endTick, float distance, float amplitude,
                                   int cycles, String forwardAxis, float height, int ticksPerKeyframe) {
            finalizeCurrentKeyframe();

            int duration = endTick - startTick;
            int numKeyframes = Math.max(2, duration / ticksPerKeyframe);

            for (int i = 0; i <= numKeyframes; i++) {
                int tick = startTick + (i * duration / numKeyframes);
                float progress = (float) i / numKeyframes;
                float forwardPos = distance * progress;

                // Triangle wave for zigzag
                float zigzagProgress = (progress * cycles) % 1.0f;
                float perpPos = amplitude * (zigzagProgress < 0.5f ? (zigzagProgress * 4 - 1) : (3 - zigzagProgress * 4));

                currentKeyframe = new KeyframeBuilder(this, tick);
                if (forwardAxis.equalsIgnoreCase("x")) {
                    currentKeyframe.position(forwardPos, height, perpPos);
                } else {
                    currentKeyframe.position(perpPos, height, forwardPos);
                }
                track.addKeyframe(currentKeyframe.buildKeyframe());
                currentKeyframe = null;
            }

            return this;
        }

        /**
         * Creates a bouncing motion along a path
         * @param startTick Starting tick
         * @param endTick Ending tick
         * @param distance Horizontal distance to travel
         * @param bounceHeight Maximum height of each bounce
         * @param bounces Number of bounces
         * @param direction Direction to move horizontally (normalized automatically)
         * @param ticksPerKeyframe How often to create keyframes
         */
        public TrackBuilder bounce(int startTick, int endTick, float distance, float bounceHeight,
                                   int bounces, Vector3f direction, int ticksPerKeyframe) {
            finalizeCurrentKeyframe();

            Vector3f normalizedDir = new Vector3f(direction).normalize();
            int duration = endTick - startTick;
            int numKeyframes = Math.max(2, duration / ticksPerKeyframe);

            for (int i = 0; i <= numKeyframes; i++) {
                int tick = startTick + (i * duration / numKeyframes);
                float progress = (float) i / numKeyframes;

                float horizontalDist = distance * progress;
                float x = normalizedDir.x * horizontalDist;
                float z = normalizedDir.z * horizontalDist;

                // Parabolic bounce
                float bounceProgress = (progress * bounces) % 1.0f;
                float y = bounceHeight * 4 * bounceProgress * (1 - bounceProgress);

                currentKeyframe = new KeyframeBuilder(this, tick);
                currentKeyframe.position(x, y, z);
                track.addKeyframe(currentKeyframe.buildKeyframe());
                currentKeyframe = null;
            }

            return this;
        }

        /**
         * Creates a grid pattern (sweeping back and forth)
         * @param startTick Starting tick
         * @param endTick Ending tick
         * @param width Width of the grid
         * @param depth Depth of the grid
         * @param rows Number of rows to sweep
         * @param height Y offset
         * @param ticksPerKeyframe How often to create keyframes
         */
        public TrackBuilder grid(int startTick, int endTick, float width, float depth, int rows,
                                 float height, int ticksPerKeyframe) {
            finalizeCurrentKeyframe();

            int duration = endTick - startTick;
            int numKeyframes = Math.max(2, duration / ticksPerKeyframe);

            for (int i = 0; i <= numKeyframes; i++) {
                int tick = startTick + (i * duration / numKeyframes);
                float progress = (float) i / numKeyframes;

                float totalProgress = progress * rows;
                int currentRow = (int) totalProgress;
                float rowProgress = totalProgress - currentRow;

                float z = (currentRow / (float) rows) * depth;
                // Alternate direction each row
                float x = (currentRow % 2 == 0) ? rowProgress * width : (1 - rowProgress) * width;

                currentKeyframe = new KeyframeBuilder(this, tick);
                currentKeyframe.position(x, height, z);
                track.addKeyframe(currentKeyframe.buildKeyframe());
                currentKeyframe = null;
            }

            return this;
        }

        /**
         * Builder for individual keyframes
         */
        public static class KeyframeBuilder {
            private final TrackBuilder parent;
            private final int tick;
            private Vector3f translation = new Vector3f(0, 0, 0);
            private float rotationX = 0; // pitch
            private float rotationY = 0; // yaw
            private float rotationZ = 0; // roll
            private Vector3f scale = new Vector3f(1, 1, 1);

            private KeyframeBuilder(TrackBuilder parent, int tick) {
                this.parent = parent;
                this.tick = tick;
            }

            /**
             * Sets the position relative to the animation origin
             */
            public KeyframeBuilder position(float x, float y, float z) {
                this.translation = new Vector3f(x, y, z);
                return this;
            }

            /**
             * Sets the rotation using euler angles (in degrees)
             * @param pitch Rotation around X axis
             * @param yaw Rotation around Y axis
             * @param roll Rotation around Z axis
             */
            public KeyframeBuilder rotation(float pitch, float yaw, float roll) {
                this.rotationX = pitch;
                this.rotationY = yaw;
                this.rotationZ = roll;
                return this;
            }

            /**
             * Sets uniform scale
             */
            public KeyframeBuilder scale(float uniform) {
                this.scale = new Vector3f(uniform, uniform, uniform);
                return this;
            }

            /**
             * Sets scale per axis
             */
            public KeyframeBuilder scale(float x, float y, float z) {
                this.scale = new Vector3f(x, y, z);
                return this;
            }

            /**
             * Starts defining another keyframe at a different time
             */
            public KeyframeBuilder at(int tick) {
                return parent.at(tick);
            }

            /**
             * Finishes this track and returns to animation builder
             */
            public AnimationBuilder endTrack() {
                return parent.endTrack();
            }

            /**
             * Adds a new track (convenience method)
             */
            public TrackBuilder addBlockTrack(String id, Material material) {
                return parent.addBlockTrack(id, material);
            }

            /**
             * Adds a new item track (convenience method)
             */
            public TrackBuilder addItemTrack(String id, ItemStack item) {
                return parent.addItemTrack(id, item);
            }

            /**
             * Adds a callback at a specific time (convenience method)
             */
            public AnimationBuilder atTime(int tick, Runnable callback) {
                return parent.atTime(tick, callback);
            }

            /**
             * Builds the animation (convenience method)
             */
            public Animation build() {
                return parent.build();
            }

            AnimationKeyframe buildKeyframe() {
                return new AnimationKeyframe(tick, translation, rotationX, rotationY, rotationZ, scale);
            }
        }
    }
}
