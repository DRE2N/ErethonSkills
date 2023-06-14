package de.erethon.spellbook.utils;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_20_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.util.BoundingBox;

import java.util.Optional;

public class NMSUtils {

    public static ArmorStand spawnInvisibleArmorstand(Location location, boolean gravity, boolean marker, boolean invulnerable, boolean physics) {
        CraftWorld craftWorld = (CraftWorld) location.getWorld();
        Level world = craftWorld.getHandle();
        ArmorStand stand = new ArmorStand(net.minecraft.world.entity.EntityType.ARMOR_STAND, world);
        stand.setInvisible(true);
        stand.setNoGravity(!gravity);
        stand.setMarker(marker);
        stand.noPhysics = !physics;
        stand.collides = false;
        stand.setInvulnerable(invulnerable);
        stand.setPos(location.getX(), location.getY(), location.getZ());
        world.addFreshEntity(stand, CreatureSpawnEvent.SpawnReason.CUSTOM);
        return stand;
    }

    // less stupid than ItemMeta at least
    public static org.bukkit.inventory.ItemStack getItemStackWithModelData(Material material, int data) {
        ItemStack itemStack = ItemStack.fromBukkitCopy(new org.bukkit.inventory.ItemStack(material, 1));
        itemStack.getOrCreateTag().putInt("CustomModelData", data);
        return itemStack.getBukkitStack();
    }

    public static void setEntityBoundingBox(Entity entity, BoundingBox box) {
        CraftEntity craftEntity = (CraftEntity) entity;
        net.minecraft.world.entity.Entity nmsEntity = craftEntity.getHandle();
        nmsEntity.setBoundingBox(new AABB(box.getMinX(), box.getMinY(), box.getMinZ(), box.getMaxX(), box.getMaxY(), box.getMaxZ()));
    }

    public static AABB getAABB(Entity entity) {
        CraftEntity craftEntity = (CraftEntity) entity;
        net.minecraft.world.entity.Entity nmsEntity = craftEntity.getHandle();
        return nmsEntity.getBoundingBox();
    }

    public static org.bukkit.entity.ArmorStand spawnItemArmorstand(org.bukkit.inventory.ItemStack stack, Location location) {
        ArmorStand stand = spawnInvisibleArmorstand(location, false, true, true, false);
        ItemStack itemStack = CraftItemStack.asNMSCopy(stack);
        stand.setItemSlot(EquipmentSlot.HEAD, itemStack);
        return (org.bukkit.entity.ArmorStand) stand.getBukkitEntity();
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

    public static Entity addAttachment(org.bukkit.entity.LivingEntity living, Entity bukkitentity, double offset) {
        net.minecraft.world.entity.LivingEntity nms = ((CraftLivingEntity) living).getHandle();
        net.minecraft.world.entity.Entity entity= ((CraftEntity) bukkitentity).getHandle();
        entity.setInvulnerable(true);
        entity.noPhysics = true;
        if (entity instanceof LivingEntity livingEntity) {
            livingEntity.collides = false;
        }
        nms.addAttachment(entity, "head", 0, offset, 0);
        return entity.getBukkitEntity();
    }
}
