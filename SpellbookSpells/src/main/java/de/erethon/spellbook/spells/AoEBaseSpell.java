package de.erethon.spellbook.spells;

import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookSpell;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_19_R3.CraftServer;
import org.bukkit.craftbukkit.v1_19_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_19_R3.inventory.CraftItemStack;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scoreboard.Scoreboard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class AoEBaseSpell extends SpellbookSpell {

    protected Location target = null;
    CraftServer server = (CraftServer) caster.getServer();
    private final int maxDistance;
    private final boolean self;
    private final double size;
    private final int customItemDataFriendly;
    private final int customItemDataEnemy;

    private List<Integer> entityIDs = new ArrayList<>();

    protected Set<LivingEntity> entities = new HashSet<>();

    public AoEBaseSpell(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        customItemDataFriendly = spellData.getInt("customItemDataFriendly", 2);
        customItemDataEnemy = spellData.getInt("customItemDataEnemy", 1);
        maxDistance = spellData.getInt("maxDistance", 32);
        size = spellData.getDouble("size", 2);
        this.self = false;
    }

    public AoEBaseSpell(LivingEntity caster, SpellData spellData, boolean self) {
        super(caster, spellData);
        customItemDataFriendly = spellData.getInt("customItemDataFriendly", 2);
        customItemDataEnemy = spellData.getInt("customItemDataEnemy", 1);
        maxDistance = 120;
        this.self = self;
        size = spellData.getDouble("size", 2);
    }


    @Override
    public boolean onPrecast() {
        if (self) {
            caster.getLocation().add(0, -1, 0);
            return true;
        }
        Block targetBlock = caster.getTargetBlockExact(64);
        if (targetBlock != null && targetBlock.isSolid()) {
            if (targetBlock.getLocation().distanceSquared(caster.getLocation()) > maxDistance * maxDistance) {
                caster.sendParsedActionBar("<color:#ff0000>Ziel zu weit entfernt!");
                return false;
            }
            target = targetBlock.getLocation().add(0.5, 1,0.5);
            return true;
        }
        caster.sendParsedActionBar("<color:#ff0000>Kein gültiges Ziel!");
        return false;
    }

    @Override
    protected boolean onCast() {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        BlockPos pos = new BlockPos((int) target.getX(), (int) target.getY(), (int) target.getZ());

           /* for (OfflinePlayer player : team.getPlayers()) {
                sendPackets((Player) player, customItemDataFriendly, pos);
            }*/
            for (Player player : Bukkit.getOnlinePlayers()) {
                sendPackets(player, customItemDataEnemy, pos);
            }

        return true;
    }

    @Override
    protected void onTick() {
        Collection<LivingEntity> nearbyEntities = target.getNearbyLivingEntities(size);
        for (LivingEntity entity : nearbyEntities) {
            if (entities.contains(entity)) {
                continue;
            }
            entities.add(entity);
            onEnter(entity);
        }
        Iterator<LivingEntity> iterator = entities.iterator();
        while(iterator.hasNext()) {
            LivingEntity entity = iterator.next();
            if (!nearbyEntities.contains(entity)) {
                iterator.remove();
                onLeave(entity);
            }
        }
    }

    protected void onEnter(LivingEntity entity) {
    }

    protected void onLeave(LivingEntity entity) {
    }

    @Override
    protected void onTickFinish() {
        super.onTickFinish();
    }

    @Override
    protected void cleanup() {
        for (ServerPlayer player : server.getServer().getPlayerList().players) {
            for (int entityID : entityIDs) {
                player.connection.send(new ClientboundRemoveEntitiesPacket(entityID));
            }
        }
        for (LivingEntity nearbyEntity : entities) {
            onLeave(nearbyEntity);
        }
    }

    private void sendPackets(Player player, int id, BlockPos pos) {
        ServerPlayer nmsplayer = ((CraftPlayer) player).getHandle();
        Level level = ((CraftWorld) caster.getWorld()).getHandle();
        Display.ItemDisplay display = new Display.ItemDisplay(EntityType.ITEM_DISPLAY, level);
        ItemStack item = new ItemStack(Items.WHITE_DYE);
        org.bukkit.inventory.ItemStack enemyItem = CraftItemStack.asBukkitCopy(item);
        ItemMeta meta = enemyItem.getItemMeta();
        meta.setCustomModelData(id);
        enemyItem.setItemMeta(meta);
        display.setItemStack(CraftItemStack.asNMSCopy(enemyItem));
        entityIDs.add(display.getId());
        ClientboundAddEntityPacket addEntityPacket = new ClientboundAddEntityPacket(display);
        ClientboundSetEntityDataPacket dataPacket = new ClientboundSetEntityDataPacket(display.getId(), display.getEntityData().packDirty());
        nmsplayer.connection.send(addEntityPacket);
        nmsplayer.connection.send(dataPacket);
    }

    public Set<LivingEntity> getEntities() {
        return entities;
    }
}
