package de.erethon.spellbook.spells.vfx;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** A resource-pack ring whose short sections settle onto the actual terrain. */
public final class TerrainRing implements AutoCloseable {

    private final ItemDisplay[] displays;
    private final String effectId;
    private final double radius;
    private final float bandWidth;

    public TerrainRing(Location center, double radius, String effectId) {
        this(center, radius, effectId, 32, 0.34f);
    }

    public TerrainRing(Location center, double radius, String effectId, int sections, float bandWidth) {
        if (sections < 8 || sections > 96 || radius <= 0 || bandWidth <= 0) {
            throw new IllegalArgumentException("Invalid terrain ring geometry");
        }
        if (!effectId.startsWith("terrain_band_")) {
            throw new IllegalArgumentException("Terrain rings require a tileable terrain band: " + effectId);
        }
        this.displays = new ItemDisplay[sections];
        this.radius = radius;
        this.effectId = effectId;
        this.bandWidth = bandWidth;
        move(center);
    }

    public void move(Location center) {
        double width = 2 * Math.PI * radius / displays.length * 1.08;
        for (int i = 0; i < displays.length; i++) {
            double angle = (i + 0.5) * Math.PI * 2 / displays.length;
            double x = center.getX() + Math.cos(angle) * radius;
            double z = center.getZ() + Math.sin(angle) * radius;
            // Start below a typical two-block ceiling, so cave floors work too.
            Location probe = new Location(center.getWorld(), x, center.getY() + 1.8, z);
            RayTraceResult hit = center.getWorld().rayTraceBlocks(probe, new Vector(0, -1, 0),
                    5.5, FluidCollisionMode.NEVER, true);
            if (hit == null || hit.getHitBlockFace() != BlockFace.UP
                    || Math.abs(hit.getHitPosition().getY() - center.getY()) > 2.2) {
                remove(i);
                continue;
            }
            Location settled = new Location(center.getWorld(), x, hit.getHitPosition().getY() + 0.045, z);
            if (displays[i] == null || !displays[i].isValid()) {
                // Local +X runs along the tangent; local +Y points outward.
                // The previous +PI/2 placed both axes 180 degrees backward.
                Quaternionf rotation = new Quaternionf()
                        .rotateY((float) -(angle - Math.PI / 2))
                        .rotateX((float) Math.PI / 2);
                displays[i] = SkillVfx.plane(settled, effectId,
                        new Vector3f((float) width, bandWidth, 1), Display.Billboard.FIXED, rotation);
            } else {
                displays[i].teleport(settled);
            }
        }
    }

    private void remove(int index) {
        if (displays[index] != null) displays[index].remove();
        displays[index] = null;
    }

    @Override
    public void close() {
        for (int i = 0; i < displays.length; i++) remove(i);
    }
}
