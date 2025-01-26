// HCharacter.java
package de.erethon.hecate.data;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.hecate.Hecate;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class HCharacter {

    private final DatabaseManager dbManager = Hecate.getInstance().getDatabaseManager();

    private final UUID characterID;
    private final HPlayer hPlayer;
    private int level;
    private String classId;
    private Timestamp createdAt;
    private String lockedBy;
    private List<String> skills;
    private CompoundTag playerData;

    public HCharacter(UUID characterID, HPlayer hPlayer, int level, String classId, Timestamp createdAt, List<String> skills) {
        this.characterID = characterID;
        this.hPlayer = hPlayer;
        this.level = level;
        this.classId = classId;
        this.createdAt = createdAt;
        this.skills = skills;
    }

    public HCharacter(UUID characterID, HPlayer hPlayer, int level, String classId, Timestamp createdAt, String lockedBy, List<String> skills) {
        this.characterID = characterID;
        this.hPlayer = hPlayer;
        this.level = level;
        this.classId = classId;
        this.createdAt = createdAt;
        this.lockedBy = lockedBy;
        this.skills = skills;
    }

    public UUID getCharacterID() {
        return characterID;
    }

    public HPlayer getHPlayer() {
        return hPlayer;
    }

    public int getLevel() {
        return level;
    }

    public String getClassId() {
        return classId;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public String getLockedBy() {
        return lockedBy;
    }

    public List<String> getSkills() {
        return skills;
    }

    public Player getPlayer() {
        return hPlayer.getPlayer();
    }

    public CompoundTag getPlayerDataNBT() {
        return playerData;
    }

    public CompletableFuture<Void> saveToDatabase(DatabaseManager dbManager) {
        return dbManager.createOrUpdateCharacter(this).thenAccept(success -> {
            if (!success) {
                System.err.println("Failed to save character: " + getCharacterID());
            } else {
                saveCharacterPlayerData();
            }
        });
    }

    public CompletableFuture<Void> loadCharacterPlayerData() {
        return dbManager.loadCharacterPlayerData(characterID).thenAccept(data -> {
            if (data != null) {
                deserializePlayerDataFromBlob(data);
            } else {
                System.err.println("Failed to load character data: " + getCharacterID());
            }
        });
    }

    public CompletableFuture<Void> saveCharacterPlayerData() {
        return dbManager.saveCharacterPlayerData(characterID, serializePlayerDataToBlob()).thenAccept(success -> {
            if (!success) {
                System.err.println("Failed to save character data: " + getCharacterID());
            }
        });
    }

    public byte[] serializePlayerDataToBlob() {
        CraftPlayer player = (CraftPlayer) hPlayer.getPlayer();
        if (player == null) {
            MessageUtil.log("Failed to serialize player data for character " + characterID + " of player " + hPlayer.getPlayer().getName() + ": Player is null");
            return new byte[0];
        }
        try {
            CompoundTag tag = new CompoundTag();
            player.getHandle().saveWithoutId(tag);
            tag.remove("WorldUUIDLeast");
            tag.remove("WorldUUIDMost");
            tag.remove("Paper.OriginWorld");
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            NbtIo.writeCompressed(tag, outputStream);
            return outputStream.toByteArray();
        }
        catch (Exception e) {
            MessageUtil.log("Failed to serialize player data for character " + characterID + " of player " + hPlayer.getPlayer().getName());
            e.printStackTrace();
        }
        return new byte[0];
    }

    private void deserializePlayerDataFromBlob(byte[] blob) {
        CraftPlayer player = (CraftPlayer) hPlayer.getPlayer();
        if (player == null) {
            MessageUtil.log("Failed to deserialize player data for character " + characterID + " of player " + hPlayer.getPlayer().getName() + ": Player is null");
            return;
        }
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(blob);
            CompoundTag tag = NbtIo.readCompressed(inputStream, NbtAccounter.unlimitedHeap());
            playerData = tag;
            // Load the player data on the main thread
            BukkitRunnable runnable = new BukkitRunnable() {
                @Override
                public void run() {
                    // Run data through DFU
                    int dataVersion = NbtUtils.getDataVersion(tag, -1);
                    ca.spottedleaf.dataconverter.minecraft.MCDataConverter.convertTag(ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry.PLAYER, tag, dataVersion, net.minecraft.SharedConstants.getCurrentVersion().getDataVersion().getVersion());
                    // Load the player data
                    player.getHandle().load(tag);
                    // Resend possibly desynced data
                    ServerPlayer handle = player.getHandle();
                    handle.onUpdateAbilities();
                    handle.refreshEntityData(handle);
                    player.removePotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS);
                    handle.connection.internalTeleport(net.minecraft.world.entity.PositionMoveRotation.of(handle), java.util.Collections.emptySet());
                    net.minecraft.server.players.PlayerList playerList = handle.server.getPlayerList();
                    playerList.sendPlayerPermissionLevel(handle, false);
                    playerList.sendLevelInfo(handle, (ServerLevel) handle.level());
                    playerList.sendAllPlayerInfo(handle); // Selected slot, inventory and some other stuff
                    handle.connection.send(new net.minecraft.network.protocol.game.ClientboundSetExperiencePacket(handle.experienceProgress, handle.totalExperience, handle.experienceLevel));
                    for (net.minecraft.world.effect.MobEffectInstance mobEffect : handle.getActiveEffects()) {
                        handle.connection.send(new net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket(handle.getId(), mobEffect, false));
                    }
                }
            };
            runnable.runTask(Hecate.getInstance());
        } catch (Exception e) {
            MessageUtil.log("Failed to deserialize player data for character " + characterID + " of player " + hPlayer.getPlayer().getName());
            e.printStackTrace();
        }
    }
}