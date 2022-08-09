package de.erethon.spellbook.spells;

import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookSpell;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_19_R1.CraftServer;
import org.bukkit.craftbukkit.v1_19_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_19_R1.inventory.CraftItemStack;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AoEBaseSpell extends SpellbookSpell {

    protected Location target = null;
    CraftServer server = (CraftServer) caster.getServer();
    private final int maxDistance;
    private final int size;
    private final int customItemDataFriendly;
    private List<Integer> customItemDataFriendlyBig;
    private final int customItemDataEnemy;
    private List<Integer> customItemDataEnemyBig;

    private List<Integer> entityIDs = new ArrayList<>();

    protected Set<LivingEntity> entities = new HashSet<>();

    public AoEBaseSpell(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        customItemDataFriendly = spellData.getInt("customItemDataFriendly", 0);
        customItemDataEnemy = spellData.getInt("customItemDataEnemy", 0);
        maxDistance = spellData.getInt("maxDistance", 32);
        if (spellData.contains("customItemDataFriendlyBig")) {
            customItemDataFriendlyBig = spellData.getIntegerList("customItemDataFriendlyBig");
        }
        if (spellData.contains("customItemDataEnemyBig")) {
            customItemDataEnemyBig = spellData.getIntegerList("customItemDataEnemyBig");
        }
        size = spellData.getInt("size", 1);
    }

    @Override
    public boolean onPrecast() {
        Block targetBlock = caster.getTargetBlockExact(64);
        if (targetBlock != null && targetBlock.isSolid()) {
            if (targetBlock.getLocation().distanceSquared(caster.getLocation()) > maxDistance * maxDistance) {
                caster.sendActionbar("<color:#ff0000>Ziel zu weit entfernt!");
                return false;
            }
            target = targetBlock.getLocation().add(0, 1,0);
            return true;
        }
        caster.sendActionbar("<color:#ff0000>Kein gültiges Ziel!");
        return false;
    }

    @Override
    protected boolean onCast() {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        BlockPos pos = new BlockPos(target.getX(), target.getY(), target.getZ());
        Team team = scoreboard.getEntityTeam(caster);
        if (team == null) {
            team = scoreboard.registerNewTeam(caster.getName());
        } // TODO
        if (size < 2) {
            for (OfflinePlayer player : team.getPlayers()) {
                sendBigAoE((Player) player, pos, true);
            }
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (team.hasPlayer(player)) {
                    continue;
                }
                sendBigAoE(player, pos, false);
            }
        } else {
            for (OfflinePlayer player : team.getPlayers()) {
                sendPackets((Player) player, customItemDataFriendly, pos);
            }
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (team.hasPlayer(player)) {
                    continue;
                }
                sendPackets(player, customItemDataEnemy, pos);
            }
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
        for (LivingEntity entity : entities) {
            if (!nearbyEntities.contains(entity)) {
                onLeave(entity);
                entities.remove(entity);
            }
        }
    }

    protected void onEnter(LivingEntity entity) {
    }

    protected void onLeave(LivingEntity entity) {
    }

    @Override
    protected void onTickFinish() {
        for (ServerPlayer player : server.getServer().getPlayerList().players) {
            for (int entityID : entityIDs) {
                player.connection.send(new ClientboundRemoveEntitiesPacket(entityID));
            }
        }
    }

    private void sendBigAoE(Player player, BlockPos pos, boolean friendly) {
        sendPackets(player, (friendly ? customItemDataFriendlyBig.get(0) : customItemDataEnemyBig.get(0)), pos.offset(1, 0, 1));
        sendPackets(player, (friendly ? customItemDataFriendlyBig.get(1) : customItemDataEnemyBig.get(1)), pos.offset(0, 0, 1));
        sendPackets(player, (friendly ? customItemDataFriendlyBig.get(2) : customItemDataEnemyBig.get(2)), pos.offset(1, 0, 0));
        sendPackets(player, (friendly ? customItemDataFriendlyBig.get(3) : customItemDataEnemyBig.get(3)), pos);
    }

    private void sendPackets(Player player, int id, BlockPos pos) {
        ServerPlayer nmsplayer = ((CraftPlayer) player).getHandle();
        Level level = ((CraftWorld) caster.getWorld()).getHandle();
        ItemFrame frame = new ItemFrame(level, pos, Direction.UP);
        //frame.setInvisible(true);
        ItemStack item = new ItemStack(Items.WHITE_DYE);
        org.bukkit.inventory.ItemStack enemyItem = CraftItemStack.asBukkitCopy(item);
        ItemMeta meta = enemyItem.getItemMeta();
        meta.setCustomModelData(id);
        enemyItem.setItemMeta(meta);
        //frame.setItem(item, true, false);
        frame.fixed = true;
        frame.setInvulnerable(true);
        entityIDs.add(frame.getId());
        ClientboundAddEntityPacket addEntityPacket = new ClientboundAddEntityPacket(frame);
        ClientboundSetEntityDataPacket dataPacket = new ClientboundSetEntityDataPacket(frame.getId(), frame.getEntityData(), true);
        ClientboundMoveEntityPacket moveEntityPacket = new ClientboundMoveEntityPacket.Rot(frame.getId(), (byte) 0, (byte) -64, false);
        nmsplayer.connection.send(addEntityPacket);
        nmsplayer.connection.send(dataPacket);
        nmsplayer.connection.send(moveEntityPacket);
    }
}