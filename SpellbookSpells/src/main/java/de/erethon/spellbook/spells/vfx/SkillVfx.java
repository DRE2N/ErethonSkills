package de.erethon.spellbook.spells.vfx;

import io.papermc.paper.datacomponent.DataComponentTypes;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Objects;

/** Shared bridge from named skill-VFX assets to client-rendered item displays. */
public final class SkillVfx {

    // Hecate sets this after the optional skill-VFX pack loads.
    public static final String LOADED_METADATA = "erethon_skill_vfx_loaded";

    private SkillVfx() {
    }

    public static boolean availableTo(LivingEntity caster) {
        return caster instanceof Player player && player.hasMetadata(LOADED_METADATA);
    }

    public static ItemStack item(String effectId) {
        NamespacedKey key = Objects.requireNonNull(NamespacedKey.fromString("erethon:vfx/" + effectId),
                "Invalid VFX effect id: " + effectId);
        ItemStack item = new ItemStack(Material.PAPER);
        item.setData(DataComponentTypes.ITEM_MODEL, key);
        return item;
    }

    /** A flat effect that remains readable from any first-person view angle. */
    public static ItemDisplay billboard(Location location, String effectId, float scale) {
        return plane(location, effectId, scale, Display.Billboard.CENTER, new Quaternionf());
    }

    /** Orients the painted texture's right-facing tip (+X) along travel in world space. */
    public static ItemDisplay projectile(Location location, Vector direction, String effectId, float scale) {
        return plane(location, effectId, scale, Display.Billboard.FIXED, projectileRotation(direction));
    }

    /** Update a projectile whose travel vector changes, such as a returning blade. */
    public static void orientProjectile(ItemDisplay display, Vector direction) {
        if (display == null || !display.isValid()) return;
        Transformation current = display.getTransformation();
        display.setTransformation(new Transformation(current.getTranslation(), projectileRotation(direction),
                current.getScale(), current.getRightRotation()));
    }

    public static Quaternionf projectileRotation(Vector direction) {
        if (direction.lengthSquared() < 0.000001) return new Quaternionf();
        Vector unit = direction.clone().normalize();
        double horizontal = Math.hypot(unit.getX(), unit.getZ());
        return new Quaternionf()
                .rotateY((float) -Math.atan2(unit.getZ(), unit.getX()))
                .rotateZ((float) Math.atan2(unit.getY(), horizontal));
    }

    /** A flat effect on the ground, suited to circles and telegraphs. */
    public static ItemDisplay groundPlane(Location location, String effectId, float scale) {
        return plane(location, effectId, scale, Display.Billboard.FIXED,
                new Quaternionf().rotateX((float) Math.PI / 2f));
    }

    /** A flat effect with a caller-chosen orientation and billboard behavior. */
    public static ItemDisplay plane(Location location, String effectId, float scale,
                                    Display.Billboard billboard, Quaternionf rotation) {
        return plane(location, effectId, new Vector3f(scale), billboard, rotation);
    }

    public static ItemDisplay plane(Location location, String effectId, Vector3f size,
                                    Display.Billboard billboard, Quaternionf rotation) {
        Location spawn = location.clone();
        spawn.setYaw(0);
        spawn.setPitch(0);
        return spawn.getWorld().spawn(spawn, ItemDisplay.class, display -> {
            display.setItemStack(item(effectId));
            display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            display.setPersistent(false);
            display.setBrightness(new Display.Brightness(15, 15));
            display.setBillboard(billboard);
            display.setViewRange(1.5f);
            display.setTeleportDuration(2);
            display.setInterpolationDuration(2);
            display.setInterpolationDelay(0);
            display.setTransformation(new Transformation(new Vector3f(), new Quaternionf(rotation),
                    new Vector3f(size), new Quaternionf()));
        });
    }
}
