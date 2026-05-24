package de.erethon.hecate.arenas;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

public record ArenaLocation(String world, double x, double y, double z, float yaw, float pitch) {

    public static ArenaLocation from(Location location) {
        return new ArenaLocation(location.getWorld().getName(), location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
    }

    public static ArenaLocation load(ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        return new ArenaLocation(
                section.getString("world"),
                section.getDouble("x"),
                section.getDouble("y"),
                section.getDouble("z"),
                (float) section.getDouble("yaw"),
                (float) section.getDouble("pitch")
        );
    }

    public void save(ConfigurationSection section) {
        section.set("world", world);
        section.set("x", x);
        section.set("y", y);
        section.set("z", z);
        section.set("yaw", yaw);
        section.set("pitch", pitch);
    }

    public Location toBukkit() {
        World bukkitWorld = Bukkit.getWorld(world);
        if (bukkitWorld == null) {
            return null;
        }
        return new Location(bukkitWorld, x, y, z, yaw, pitch);
    }

    public String compact() {
        return world + " " + Math.round(x * 10.0) / 10.0 + " " + Math.round(y * 10.0) / 10.0 + " " + Math.round(z * 10.0) / 10.0;
    }
}
