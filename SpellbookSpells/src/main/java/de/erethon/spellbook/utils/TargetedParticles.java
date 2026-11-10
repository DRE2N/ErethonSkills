package de.erethon.spellbook.utils;

import com.destroystokyo.paper.ParticleBuilder;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/** Particle effects attached to an entity should be visible to others, but not to that player. */
public final class TargetedParticles {

    private TargetedParticles() {
    }

    public static void spawnForOthers(Particle particle, LivingEntity subject, Location location,
                                      int count, double offsetX, double offsetY, double offsetZ) {
        spawnForOthers(particle, subject, location, count, offsetX, offsetY, offsetZ, 0);
    }

    public static void spawnForOthers(Particle particle, LivingEntity subject, Location location,
                                      int count, double offsetX, double offsetY, double offsetZ, double extra) {
        spawnForOthers(particle, subject, location, count, offsetX, offsetY, offsetZ, extra, null);
    }

    public static void spawnForOthers(Particle particle, LivingEntity subject, Location location,
                                      int count, double offsetX, double offsetY, double offsetZ, double extra, Object data) {
        ParticleBuilder builder = new ParticleBuilder(particle)
                .location(location)
                .count(count)
                .offset(offsetX, offsetY, offsetZ)
                .extra(extra);
        if (data != null) {
            builder.data(data);
        }
        builder.receivers(location.getWorld().getNearbyPlayers(location, 48,
                player -> !(subject instanceof Player) || player != subject));
        builder.spawn();
    }
}
