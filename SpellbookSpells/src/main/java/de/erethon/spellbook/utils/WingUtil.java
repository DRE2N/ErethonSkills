package de.erethon.spellbook.utils;

import com.destroystokyo.paper.ParticleBuilder;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class WingUtil {
    private static final Point3D[] outline = {
        new Point3D(0, 0, -0.5f),
        new Point3D(0.1f, 0.01f, -0.5f),
        new Point3D(0.3f, 0.03f, -0.5f),
        new Point3D(0.4f, 0.04f, -0.5f),
        new Point3D(0.6f, 0.1f, -0.5f),
        new Point3D(0.61f, 0.2f, -0.5f),
        new Point3D(0.62f, 0.4f, -0.5f),
        new Point3D(0.63f, 0.6f, -0.5f),
        new Point3D(0.635f, 0.7f, -0.5f),
        new Point3D(0.7f, 0.7f, -0.5f),
        new Point3D(0.9f, 0.75f, -0.5f),
        new Point3D(1.2f, 0.8f, -0.5f),
        new Point3D(1.4f, 0.9f, -0.5f),
        new Point3D(1.6f, 1f, -0.5f),
        new Point3D(1.8f, 1.1f, -0.5f),
        new Point3D(1.85f, 0.9f, -0.5f),
        new Point3D(1.9f, 0.7f, -0.5f),
        new Point3D(1.85f, 0.5f, -0.5f),
        new Point3D(1.8f, 0.3f, -0.5f),
        new Point3D(1.75f, 0.1f, -0.5f),
        new Point3D(1.7f, -0.1f, -0.5f),
        new Point3D(1.65f, -0.3f, -0.5f),
        new Point3D(1.55f, -0.5f, -0.5f),
        new Point3D(1.45f, -0.7f, -0.5f),
        new Point3D(1.30f, -0.75f, -0.5f),
        new Point3D(1.15f, -0.8f, -0.5f),
        new Point3D(1.0f, -0.85f, -0.5f),
        new Point3D(0.8f, -0.87f, -0.5f),
        new Point3D(0.6f, -0.7f, -0.5f),
        new Point3D(0.5f, -0.5f, -0.5f),
        new Point3D(0.4f, -0.3f, -0.5f),
        new Point3D(0.3f, -0.3f, -0.5f),
        new Point3D(0.15f, -0.3f, -0.5f),
        new Point3D(0f, -0.3f, -0.5f),

        //
        new Point3D(0.9f, 0.55f, -0.5f),
        new Point3D(1.2f, 0.6f, -0.5f),
        new Point3D(1.4f, 0.7f, -0.5f),
        new Point3D(1.6f, 0.9f, -0.5f),
        //
        new Point3D(0.9f, 0.35f, -0.5f),
        new Point3D(1.2f, 0.4f, -0.5f),
        new Point3D(1.4f, 0.5f, -0.5f),
        new Point3D(1.6f, 0.7f, -0.5f),
        //
        new Point3D(0.9f, 0.15f, -0.5f),
        new Point3D(1.2f, 0.2f, -0.5f),
        new Point3D(1.4f, 0.3f, -0.5f),
        new Point3D(1.6f, 0.5f, -0.5f),
        //
        new Point3D(0.9f, -0.05f, -0.5f),
        new Point3D(1.2f, 0f, -0.5f),
        new Point3D(1.4f, 0.1f, -0.5f),
        new Point3D(1.6f, 0.3f, -0.5f),
        //
        new Point3D(0.7f, -0.25f, -0.5f),
        new Point3D(1.0f, -0.2f, -0.5f),
        new Point3D(1.2f, -0.1f, -0.5f),
        new Point3D(1.4f, 0.1f, -0.5f),
        //
        new Point3D(0.7f, -0.45f, -0.5f),
        new Point3D(1.0f, -0.4f, -0.5f),
        new Point3D(1.2f, -0.3f, -0.5f),
        new Point3D(1.4f, -0.1f, -0.5f),
        //
        new Point3D(1.30f, -0.55f, -0.5f),
        new Point3D(1.15f, -0.6f, -0.5f),
        new Point3D(1.0f, -0.65f, -0.5f)
    };

    private static final Point3D[] fill = {
        new Point3D(1.2f, 0.6f, -0.5f),
        new Point3D(1.4f, 0.7f, -0.5f),

        new Point3D(1.1f, 0.2f, -0.5f),
        new Point3D(1.3f, 0.3f, -0.5f),

        new Point3D(1.0f, -0.2f, -0.5f),
        new Point3D(1.2f, -0.1f, -0.5f),
    };

    public static void displayWings(Player player, Color outlineColor, Color fillColor, float size) {
        Location playerLocation = player.getEyeLocation();
        World playerWorld = player.getWorld();
        float x = (float) playerLocation.getX();
        float y = (float) playerLocation.getY() - 0.2f;
        float z = (float) playerLocation.getZ();
        float bodyRot = -player.getYaw() * 0.017453292F;
        Point3D rotated;
        for(Point3D point : outline) {
            rotated = point.rotate(bodyRot).multiply(size);
            new ParticleBuilder(Particle.DUST).location(new Location(playerWorld, rotated.x + x, rotated.y + y, rotated.z +z)).allPlayers().color(outlineColor).spawn();
            point.z *= -1;
            rotated = point.rotate(bodyRot + 3.1415f).multiply(size);
            point.z *= -1;
            new ParticleBuilder(Particle.DUST).location(new Location(playerWorld, rotated.x + x, rotated.y + y, rotated.z +z)).allPlayers().color(outlineColor).spawn();
        }

        for(Point3D point : fill) {
            rotated = point.rotate(bodyRot).multiply(size);
            new ParticleBuilder(Particle.DUST).location(new Location(playerWorld, rotated.x + x, rotated.y + y, rotated.z + z)).allPlayers().color(fillColor).spawn();
            point.z *= -1;
            rotated = point.rotate(bodyRot + 3.1415f).multiply(size);
            point.z *= -1;
            new ParticleBuilder(Particle.DUST).location(new Location(playerWorld, rotated.x + x, rotated.y + y , rotated.z + z)).allPlayers().color(fillColor).spawn();
        }
    }
}
