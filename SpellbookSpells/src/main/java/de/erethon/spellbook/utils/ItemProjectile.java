package de.erethon.spellbook.utils;

import de.erethon.spellbook.api.SpellbookSpell;
import de.erethon.spellbook.events.ItemProjectileHitEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public class ItemProjectile extends Arrow {

    org.bukkit.entity.ArmorStand armorStand;

    net.minecraft.world.entity.decoration.ArmorStand armorStandNMS;
    org.bukkit.entity.Arrow arrow;
    ItemStack itemStack;
    SpellbookSpell spell;

    public ItemProjectile(org.bukkit.inventory.ItemStack item, double x, double y, double z, World world, SpellbookSpell spell) {
        super(((CraftWorld) world).getHandle(), x, y, z, CraftItemStack.asNMSCopy(item), CraftItemStack.asNMSCopy(item));
        this.spell = spell;
        net.minecraft.world.entity.decoration.ArmorStand nms = NMSUtils.spawnInvisibleArmorstand(new Location(world, x, y, z), false, true, true, false);
        itemStack = CraftItemStack.asNMSCopy(item);
        nms.setItemSlot(EquipmentSlot.HEAD, itemStack);
        armorStandNMS = nms;
        armorStand = (ArmorStand) nms.getBukkitEntity();
        arrow = (org.bukkit.entity.Arrow) getBukkitEntity();
        persist = false;
        ServerLevel level = ((CraftWorld) world).getHandle().getLevel();
        level.addFreshEntity(this);
        setSilent(true);
        setBaseDamage(0);
        level.getServer().getPlayerList().broadcastAll(new ClientboundRemoveEntitiesPacket(this.getId()));
        setLife(400); // maybe make this configurable?
    }

    public void setLife(int life) {
        this.life = life;
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        armorStand.remove();
    }

    @Override
    public void move(MoverType movementType, Vec3 movement) {
        super.move(movementType, movement);
        updateArmorstand();
    }

    @Override
    public void moveRelative(float speed, Vec3 movementInput) {
        super.moveRelative(speed, movementInput);
        updateArmorstand();
    }

    @Override
    public void setPos(double x, double y, double z) {
        super.setPos(x, y, z);
        updateArmorstand();
    }

    @Override
    protected void moveTowardsClosestSpace(double x, double y, double z) {
        super.moveTowardsClosestSpace(x, y, z);
        updateArmorstand();
    }

    @Override
    public void tick() {
        super.tick();
        updateArmorstand();
    }

    @Override
    public void baseTick() {
        super.baseTick();
        updateArmorstand();
    }

    public void updateArmorstand() {
        armorStand.teleport(arrow.getLocation().add(0, -0.5, 0));
        armorStand.setHeadPose(convertVectorToEulerAngle(arrow.getLocation().getDirection())); // TODO: Something here isn't right
    }

    @Override
    public ProjectileDeflection preHitTargetOrDeflectSelf(HitResult hitResult) {
        Entity entity = null;
        if (hitResult.getType() == HitResult.Type.ENTITY) {
            EntityHitResult result = (EntityHitResult) hitResult;
            entity = result.getEntity().getBukkitEntity();
        }
        ItemProjectileHitEvent event = new ItemProjectileHitEvent(spell, this, arrow, entity);
        Bukkit.getServer().getPluginManager().callEvent(event);
        return super.preHitTargetOrDeflectSelf(hitResult);
    }

    @Override
    public ItemStack getPickupItem() {
        return itemStack;
    }


    private static EulerAngle convertVectorToEulerAngle(Vector vec) {

        double x = vec.getX();
        double y = vec.getY();
        double z = vec.getZ();

        double xz = Math.sqrt(x * x + z * z);

        double eulX;
        if (x < 0) {
            if (y == 0) {
                eulX = Math.PI * 0.5;
            } else {
                eulX = Math.atan(xz / y) + Math.PI;
            }
        } else {
            eulX = Math.atan(y / xz) + Math.PI * 0.5;
        }
        double eulY;
        if (x == 0) {
            if (z > 0) {
                eulY = Math.PI;
            } else {
                eulY = 0;
            }
        } else {
            eulY = Math.atan(z / x) + Math.PI * 0.5;
        }

        return new EulerAngle(eulX, eulY, 0);
    }


}
