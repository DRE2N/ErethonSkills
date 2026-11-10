package de.erethon.spellbook.spells.vfx;

import org.bukkit.Material;

/** Segment geometry for glass ground markers shared by the class VFX helpers. */
public final class GlassRingSegments {

    private GlassRingSegments() {
    }

    public static boolean isGlass(Material material) {
        return material.name().endsWith("GLASS");
    }

    public static int count(int requested, Material material) {
        if (!isGlass(material)) {
            return requested;
        }
        int denser = (int) Math.ceil(requested * 1.5);
        return 2 * (denser + denser % 2);
    }

    public static float length(double radius, int count, float width, float requested, Material material) {
        if (!isGlass(material) || count < 3) {
            return requested;
        }
        // Match the chord at the strip's centerline. Extend halfway toward the
        // outer-edge join to hide tiny gaps without stacking glass end faces.
        double chordCenterRadius = radius * Math.cos(Math.PI / count);
        double joinRadius = Math.max(0.01, chordCenterRadius + width / 4.0);
        return (float) Math.max(0.01, 2 * joinRadius * Math.tan(Math.PI / count));
    }
}
