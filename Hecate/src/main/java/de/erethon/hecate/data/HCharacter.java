// HCharacter.java
package de.erethon.hecate.data;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.hecate.Hecate;
import de.erethon.hecate.casting.CharacterCastingManager;
import de.erethon.hecate.classes.HClass;
import de.erethon.hecate.events.CombatModeReason;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.Timestamp;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class HCharacter {

    private final Hecate plugin = Hecate.getInstance();
    private final DatabaseManager dbManager = plugin.getDatabaseManager();

    private final UUID characterID;
    private final HPlayer hPlayer;
    private int level;
    private String classId;
    private HClass hClass;
    private Timestamp createdAt;
    private String lockedBy;
    private List<String> skills;
    private CompoundTag playerData;
    private boolean saveInventory = true;
    private boolean shouldSaveHotbarSeparately = false;

    private CharacterCastingManager castingManager;
    private boolean isInCastMode = false;
    private ItemStack[] hotbar = new ItemStack[9];
    ListTag hotbarTag = new ListTag();

    public HCharacter(UUID characterID, HPlayer hPlayer, int level, String classId, Timestamp createdAt, List<String> skills) {
        this.characterID = characterID;
        this.hPlayer = hPlayer;
        this.level = level;
        this.classId = classId;
        this.createdAt = createdAt;
        this.skills = skills;
        hClass = plugin.getHClass(classId);
        if (hClass == null) {
            MessageUtil.log("Failed to load class for character " + characterID + ": " + classId + ", defaulting to assassin");
            hClass = plugin.getHClass("assassin");
        }
    }

    public HCharacter(UUID characterID, HPlayer hPlayer, int level, String classId, Timestamp createdAt, String lockedBy, List<String> skills) {
        this.characterID = characterID;
        this.hPlayer = hPlayer;
        this.level = level;
        this.classId = classId;
        this.createdAt = createdAt;
        this.lockedBy = lockedBy;
        this.skills = skills;
        hClass = plugin.getHClass(classId);
        if (hClass == null) {
            MessageUtil.log("Failed to load class for character " + characterID + ": " + classId + ", defaulting to assassin");
            hClass = plugin.getHClass("assassin");
        }
    }

    public void switchCastMode(CombatModeReason reason, boolean newMode) {
        if (castingManager == null) {
            castingManager = new CharacterCastingManager(this);
        }

        if (newMode) {
            // Save the hotbar
            for (int i = 0; i < 9; i++) {
                hotbar[i] = getPlayer().getInventory().getItem(i);
            }
            ItemStack weapon = getPlayer().getInventory().getItemInMainHand();
            shouldSaveHotbarSeparately = true;
            // Save the rest of player data
            CompletableFuture.runAsync(() -> saveCharacterPlayerData(true).join())
                    .thenRun(() -> {
                        // Back on the main thread
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            try {
                                castingManager.switchCastMode(reason, true, weapon);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            isInCastMode = true;
                            MessageUtil.sendMessage(getPlayer(), "Entered cast mode");
                        });
                    });
        } else {
            castingManager.switchCastMode(reason, false, null);
            isInCastMode = false;
            MessageUtil.sendMessage(getPlayer(), "Left cast mode");
            for (int i = 0; i < 9; i++) {
                getPlayer().getInventory().setItem(i, hotbar[i]);
            }
            shouldSaveHotbarSeparately = false;
        }
    }


    public boolean isInCastMode() {
        return isInCastMode;
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

    public HClass getHClass() {
        return hClass;
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
                MessageUtil.log("Failed to save character: " + getCharacterID());
            } else {
                saveCharacterPlayerData(false);
            }
        });
    }

    public CompletableFuture<Void> loadCharacterPlayerData() {
        return dbManager.loadCharacterPlayerData(characterID).thenAccept(data -> {
            if (data != null) {
                deserializePlayerDataFromBlob(data);
                MessageUtil.log("Loaded character player data: " + getCharacterID() + " of player " + hPlayer.getPlayer().getName());
            } else {
                MessageUtil.log("Failed to load character data: " + getCharacterID());
            }
        });
    }

    public CompletableFuture<Void> saveCharacterPlayerData(boolean castModeSwitch) {
        MessageUtil.log("Thread: " + Thread.currentThread().getName());
        if (!saveInventory) {
            return CompletableFuture.runAsync(() -> {});
        }
        return dbManager.saveCharacterPlayerData(characterID, serializePlayerDataToBlob(castModeSwitch)).thenAccept(success -> {
            if (!success) {
                System.err.println("Failed to save character data: " + getCharacterID());
            } else {
                MessageUtil.log("Saved player data for character " + characterID + " of player " + hPlayer.getPlayer().getName());
            }
        });
    }

    public byte[] serializePlayerDataToBlob(boolean castModeSwitch) {
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
            // Add hotbar items if we are going to cast mode
            if (shouldSaveHotbarSeparately || castModeSwitch) {
                hotbarTag = new ListTag();
                for (int i = 0; i < 9; i++) {
                    if (hotbar[i] == null) { // Empty slot
                        hotbarTag.add(StringTag.valueOf("empty"));
                        continue;
                    }
                    String item = Base64.getEncoder().encodeToString(hotbar[i].serializeAsBytes());
                    hotbarTag.add(StringTag.valueOf(item));
                }
                MessageUtil.log("Saved hotbar separately: " + hotbarTag.size());
                shouldSaveHotbarSeparately = false; // We only want to update the tag once, otherwise it gets overridden with the cast mode items
            }
            if (!hotbarTag.isEmpty()) { // If we have hotbar items, save them with the rest
                tag.put("HecateHotbar", hotbarTag);
            }
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
                    boolean wasInCastMode = tag.contains("HecateHotbar");
                    if (wasInCastMode) { // If the player was in cast mode, we need to ensure positions match the current player's
                        try { // Don't fail completely if we can't update the player's position
                            ServerPlayer sp = player.getHandle();
                            ListTag pos = tag.getList("Pos", 6); // 6 -> DoubleTag
                            ListTag motion = tag.getList("Motion", 6); // 6 -> DoubleTag
                            ListTag rotation = tag.getList("Rotation", 6); // 6 -> DoubleTag
                            if (pos.size() == 3) {
                                pos.set(0, DoubleTag.valueOf(sp.getX()));
                                pos.set(1, DoubleTag.valueOf(sp.getY()));
                                pos.set(2, DoubleTag.valueOf(sp.getZ()));
                            }
                            if (motion.size() == 3) { // sometimes the player's motion is incomplete?
                                motion.set(0, DoubleTag.valueOf(sp.getDeltaMovement().x()));
                                motion.set(1, DoubleTag.valueOf(sp.getDeltaMovement().y()));
                                motion.set(2, DoubleTag.valueOf(sp.getDeltaMovement().z()));
                            }
                            if (rotation.size() == 2) {
                                rotation.set(0, DoubleTag.valueOf(sp.getYRot()));
                                rotation.set(1, DoubleTag.valueOf(sp.getXRot()));
                            }
                            tag.put("FallDistance", FloatTag.valueOf(sp.fallDistance)); // We don't want players to cheat by switching to cast mode in mid-air
                        } catch (Exception e) {
                            MessageUtil.log("Failed to update player position for character " + characterID + " of player " + hPlayer.getPlayer().getName() + ": " + e.getMessage());
                        }
                    }
                    player.getHandle().load(tag);
                    // If the player was in cast mode, the items should be restored
                    if (wasInCastMode) {
                        MessageUtil.log("Player disconnected in cast mode, restoring hotbar");
                        ListTag listTag = tag.getList("HecateHotbar", 8); // 8 -> StringTag
                        for (int i = 0; i < 9; i++) {
                            if (listTag.getString(i).equals("empty")) {
                                hotbar[i] = null;
                                player.getInventory().setItem(i, null);
                                continue;
                            }
                            byte[] bytes = Base64.getDecoder().decode(listTag.getString(i));
                            if (bytes.length == 0) { // Something went wrong, probably an empty slot
                                player.getInventory().setItem(i, null);
                                continue;
                            }
                            hotbar[i] = ItemStack.deserializeBytes(bytes);
                            player.getInventory().setItem(i, hotbar[i]);
                        }
                        tag.remove("HecateHotbar"); // We successfully restored the hotbar, so we don't need to do it again
                        MessageUtil.log("Successfully restored hotbar for player.");
                    }
                    // Resend possibly desynced data
                    ServerPlayer handle = player.getHandle();
                    handle.onUpdateAbilities();
                    handle.refreshEntityData(handle);
                    player.removePotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS);
                    if (!wasInCastMode) { // If the player was in cast mode, we don't want to teleport them
                        handle.connection.internalTeleport(net.minecraft.world.entity.PositionMoveRotation.of(handle), java.util.Collections.emptySet());
                    }
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