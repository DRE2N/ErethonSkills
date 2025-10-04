package de.erethon.hecate.charselection;

import com.destroystokyo.paper.profile.CraftPlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.datafixers.util.Pair;
import de.erethon.hecate.Hecate;
import net.minecraft.core.NonNullList;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class BaseDisplay {

    private final Hecate plugin = Hecate.getInstance();

    private UUID uuid;
    protected int entityId;
    protected NonNullList<ItemStack> items = NonNullList.withSize(36, ItemStack.EMPTY);
    protected NonNullList<ItemStack> armor = NonNullList.withSize(4, ItemStack.EMPTY);
    protected NonNullList<ItemStack> offhand = NonNullList.withSize(1, ItemStack.EMPTY);
    protected int selectedSlot = 1;

    protected final BaseSelection selection;

    public BaseDisplay(BaseSelection selection) {
        this.selection = selection;
    }

    public void display(Player player, Location location) {
        uuid = UUID.randomUUID();
        entityId = Bukkit.getUnsafe().nextEntityId();
        sendPlayerPackets(player, location);
    }

    public void remove(Player player) {
        CraftPlayer craftPlayer = (CraftPlayer) player;
        net.minecraft.server.level.ServerPlayer serverPlayer = craftPlayer.getHandle();
        serverPlayer.connection.send(new ClientboundRemoveEntitiesPacket(entityId));
    }

    private void sendPlayerPackets(Player player, Location location) {
        try { // Futures swallow exceptions otherwise
            double x = location.getX();
            double y = location.getY();
            double z = location.getZ();
            float yaw = location.getYaw();
            float pitch = location.getPitch();
            CraftPlayer craftPlayer = (CraftPlayer) player;
            net.minecraft.server.level.ServerPlayer serverPlayer = craftPlayer.getHandle();
            ServerGamePacketListenerImpl connection = craftPlayer.getHandle().connection;
            // Create a new GameProfile with the same properties as the player's profile, so the client can render the skin
            Set<ProfileProperty> properties = player.getPlayerProfile().getProperties();
            CraftPlayerProfile craftPlayerProfile = new CraftPlayerProfile(uuid, "CharSel");
            craftPlayerProfile.getProperties().addAll(properties);
            // Send info to the client to display the player
            Mannequin mannequin = new Mannequin(EntityType.MANNEQUIN, serverPlayer.level());
            mannequin.setProfile(craftPlayerProfile.buildResolvableProfile());

            ClientboundAddEntityPacket addEntityPacket = new ClientboundAddEntityPacket(entityId, uuid, x, y, z, pitch, yaw, EntityType.MANNEQUIN, 0, Vec3.ZERO, yaw);
            ClientboundSetEntityDataPacket entityDataPacket = new ClientboundSetEntityDataPacket(entityId, mannequin.getEntityData().packAll());
            connection.send(addEntityPacket);
            connection.send(entityDataPacket);
            sendEntityEquipmentPacket(connection);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendEntityEquipmentPacket(ServerGamePacketListenerImpl connection) {
        List<Pair<EquipmentSlot, ItemStack>> slots = NonNullList.create();
        slots.add(Pair.of(EquipmentSlot.MAINHAND, items.get(selectedSlot)));
        slots.add(Pair.of(EquipmentSlot.OFFHAND, offhand.getFirst()));
        slots.add(Pair.of(EquipmentSlot.HEAD, armor.get(3)));
        slots.add(Pair.of(EquipmentSlot.CHEST, armor.get(2)));
        slots.add(Pair.of(EquipmentSlot.LEGS, armor.get(1)));
        slots.add(Pair.of(EquipmentSlot.FEET, armor.get(0)));
        ClientboundSetEquipmentPacket equipmentPacket = new ClientboundSetEquipmentPacket(entityId, slots, true);
        connection.send(equipmentPacket);
    }
}
