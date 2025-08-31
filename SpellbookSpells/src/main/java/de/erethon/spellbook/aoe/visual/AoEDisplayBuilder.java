package de.erethon.spellbook.aoe.visual;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

/**
 * Builder for creating Display entities with AoE visual effects
 */
public class AoEDisplayBuilder {
    private final Location location;

    public AoEDisplayBuilder(Location location) {
        this.location = location.clone();
    }

    /**
     * Creates a BlockDisplay entity
     */
    public BlockDisplayBuilder blockDisplay(Material material) {
        return new BlockDisplayBuilder(location, material);
    }

    /**
     * Creates an ItemDisplay entity
     */
    public ItemDisplayBuilder itemDisplay(ItemStack item) {
        return new ItemDisplayBuilder(location, item);
    }

    /**
     * Creates a TextDisplay entity
     */
    public TextDisplayBuilder textDisplay(String text) {
        return new TextDisplayBuilder(location, text);
    }

    public static class BlockDisplayBuilder {
        private final Location location;
        private final Material material;
        private Vector3f scale = new Vector3f(1, 1, 1);
        private Vector3f translation = new Vector3f(0, 0, 0);
        private AxisAngle4f rotation = new AxisAngle4f(0, 0, 1, 0);
        private int glowColorOverride = -1;

        public BlockDisplayBuilder(Location location, Material material) {
            this.location = location;
            this.material = material;
        }

        public BlockDisplayBuilder scale(float x, float y, float z) {
            this.scale = new Vector3f(x, y, z);
            return this;
        }

        public BlockDisplayBuilder scale(float uniform) {
            return scale(uniform, uniform, uniform);
        }

        public BlockDisplayBuilder translate(float x, float y, float z) {
            this.translation = new Vector3f(x, y, z);
            return this;
        }

        public BlockDisplayBuilder rotate(float angle, float x, float y, float z) {
            this.rotation = new AxisAngle4f(angle, x, y, z);
            return this;
        }

        public BlockDisplayBuilder glowColor(int rgb) {
            this.glowColorOverride = rgb;
            return this;
        }

        public BlockDisplay build() {
            BlockDisplay display = (BlockDisplay) location.getWorld().spawnEntity(location, EntityType.BLOCK_DISPLAY);
            display.setBlock(material.createBlockData());

            Transformation transformation = new Transformation(translation, rotation, scale, new AxisAngle4f(0, 0, 1, 0));
            display.setTransformation(transformation);

            if (glowColorOverride != -1) {
                display.setGlowColorOverride(Color.fromRGB(glowColorOverride));
                display.setGlowing(true);
            }

            return display;
        }
    }

    public static class ItemDisplayBuilder {
        private final Location location;
        private final ItemStack item;
        private Vector3f scale = new Vector3f(1, 1, 1);
        private Vector3f translation = new Vector3f(0, 0, 0);
        private AxisAngle4f rotation = new AxisAngle4f(0, 0, 1, 0);
        private ItemDisplay.ItemDisplayTransform displayTransform = ItemDisplay.ItemDisplayTransform.FIXED;
        private int glowColorOverride = -1;

        public ItemDisplayBuilder(Location location, ItemStack item) {
            this.location = location;
            this.item = item;
        }

        public ItemDisplayBuilder scale(float x, float y, float z) {
            this.scale = new Vector3f(x, y, z);
            return this;
        }

        public ItemDisplayBuilder scale(float uniform) {
            return scale(uniform, uniform, uniform);
        }

        public ItemDisplayBuilder translate(float x, float y, float z) {
            this.translation = new Vector3f(x, y, z);
            return this;
        }

        public ItemDisplayBuilder rotate(float angle, float x, float y, float z) {
            this.rotation = new AxisAngle4f(angle, x, y, z);
            return this;
        }

        public ItemDisplayBuilder transform(ItemDisplay.ItemDisplayTransform transform) {
            this.displayTransform = transform;
            return this;
        }

        public ItemDisplayBuilder glowColor(int rgb) {
            this.glowColorOverride = rgb;
            return this;
        }

        public ItemDisplay build() {
            ItemDisplay display = (ItemDisplay) location.getWorld().spawnEntity(location, EntityType.ITEM_DISPLAY);
            display.setItemStack(item);
            display.setItemDisplayTransform(displayTransform);

            Transformation transformation = new Transformation(translation, rotation, scale, new AxisAngle4f(0, 0, 1, 0));
            display.setTransformation(transformation);

            if (glowColorOverride != -1) {
                display.setGlowColorOverride(Color.fromRGB(glowColorOverride));
                display.setGlowing(true);
            }

            return display;
        }
    }

    public static class TextDisplayBuilder {
        private final Location location;
        private final String text;
        private Vector3f scale = new Vector3f(1, 1, 1);
        private Vector3f translation = new Vector3f(0, 0, 0);
        private AxisAngle4f rotation = new AxisAngle4f(0, 0, 1, 0);
        private boolean billboard = true;
        private boolean seeThrough = false;
        private boolean shadowed = false;
        private int backgroundColor = 0x40000000; // Default transparent black
        private int glowColorOverride = -1;

        public TextDisplayBuilder(Location location, String text) {
            this.location = location;
            this.text = text;
        }

        public TextDisplayBuilder scale(float x, float y, float z) {
            this.scale = new Vector3f(x, y, z);
            return this;
        }

        public TextDisplayBuilder scale(float uniform) {
            return scale(uniform, uniform, uniform);
        }

        public TextDisplayBuilder translate(float x, float y, float z) {
            this.translation = new Vector3f(x, y, z);
            return this;
        }

        public TextDisplayBuilder rotate(float angle, float x, float y, float z) {
            this.rotation = new AxisAngle4f(angle, x, y, z);
            return this;
        }

        public TextDisplayBuilder billboard(boolean billboard) {
            this.billboard = billboard;
            return this;
        }

        public TextDisplayBuilder seeThrough(boolean seeThrough) {
            this.seeThrough = seeThrough;
            return this;
        }

        public TextDisplayBuilder shadowed(boolean shadowed) {
            this.shadowed = shadowed;
            return this;
        }

        public TextDisplayBuilder backgroundColor(int argb) {
            this.backgroundColor = argb;
            return this;
        }

        public TextDisplayBuilder glowColor(int rgb) {
            this.glowColorOverride = rgb;
            return this;
        }

        public TextDisplay build() {
            TextDisplay display = (TextDisplay) location.getWorld().spawnEntity(location, EntityType.TEXT_DISPLAY);
            display.text(MiniMessage.miniMessage().deserialize(text));
            display.setBillboard(billboard ? Display.Billboard.CENTER : Display.Billboard.FIXED);
            display.setSeeThrough(seeThrough);
            display.setShadowed(shadowed);
            display.setBackgroundColor(Color.fromARGB(backgroundColor));

            Transformation transformation = new Transformation(translation, rotation, scale, new AxisAngle4f(0, 0, 1, 0));
            display.setTransformation(transformation);

            if (glowColorOverride != -1) {
                display.setGlowColorOverride(Color.fromRGB(glowColorOverride));
                display.setGlowing(true);
            }

            return display;
        }
    }
}
