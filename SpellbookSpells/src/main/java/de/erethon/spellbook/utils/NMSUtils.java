package de.erethon.spellbook.utils;

import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.Level;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_19_R1.CraftWorld;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.Optional;

public class NMSUtils {

    public static ArmorStand spawnInvisibleArmorstand(Location location, boolean gravity, boolean marker, boolean invulnerable) {
        CraftWorld craftWorld = (CraftWorld) location.getWorld();
        Level world = craftWorld.getHandle();
        ArmorStand stand = new ArmorStand(net.minecraft.world.entity.EntityType.ARMOR_STAND, world);
        stand.setInvisible(true);
        stand.setNoGravity(!gravity);
        stand.setMarker(marker);
        stand.noPhysics = true;
        stand.collides = false;
        stand.setInvulnerable(invulnerable);
        stand.getBukkitEntity().teleport(location);
        world.addFreshEntity(stand, CreatureSpawnEvent.SpawnReason.CUSTOM);
        return stand;
    }

    public static net.minecraft.world.entity.Entity spawnEntityWithoutSending(Location location, org.bukkit.entity.EntityType type) {
        CraftWorld craftWorld = (CraftWorld) location.getWorld();
        Level world = craftWorld.getHandle();
        Optional<net.minecraft.world.entity.EntityType<?>> types = net.minecraft.world.entity.EntityType.byString(String.valueOf(type.getKey()));
        if (types.isPresent()) {
            net.minecraft.world.entity.EntityType<?> entityTypes = types.get();
            return entityTypes.create(world);
        }
        return null;
    }

    public static void addEntity(net.minecraft.world.entity.Entity entity, Location location) {
        entity.getBukkitEntity().teleport(location);
        Level world = entity.getCommandSenderWorld();
        world.addFreshEntity(entity, CreatureSpawnEvent.SpawnReason.CUSTOM);
    }
}
