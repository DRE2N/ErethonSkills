package de.erethon.hecate.arenas;

import org.bukkit.configuration.ConfigurationSection;

public record CapturePointDefinition(String id, ArenaLocation location, double radius, int scorePerSecond, int captureSeconds, int neutralizeSeconds) {

    public static CapturePointDefinition load(String id, ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        ArenaLocation location = ArenaLocation.load(section.getConfigurationSection("location"));
        if (location == null || location.world() == null || location.world().isBlank()) {
            return null;
        }
        return new CapturePointDefinition(
                id,
                location,
                section.getDouble("radius", 5.0),
                section.getInt("scorePerSecond", 1),
                section.getInt("captureSeconds", 8),
                section.getInt("neutralizeSeconds", 5)
        );
    }

    public void save(ConfigurationSection section) {
        section.set("radius", radius);
        section.set("scorePerSecond", scorePerSecond);
        section.set("captureSeconds", captureSeconds);
        section.set("neutralizeSeconds", neutralizeSeconds);
        location.save(section.createSection("location"));
    }
}
