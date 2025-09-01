package de.erethon.hecate.data;

import com.mojang.logging.LogUtils;
import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.hecate.Hecate;
import de.erethon.hecate.casting.CharacterCastingManager;
import de.erethon.hecate.classes.HClass;
import de.erethon.hecate.classes.Traitline;
import de.erethon.hecate.data.dao.CharacterDao;
import de.erethon.hecate.events.CombatModeReason;
import de.erethon.hecate.progression.LevelUtil;
import de.erethon.spellbook.api.SpellData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.util.CraftLocation;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.Timestamp;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class HCharacter {

    private final Hecate plugin = Hecate.getInstance();
    private final DatabaseManager dbManager = plugin.getDatabaseManager();

    private final UUID characterID;
    private final HPlayer hPlayer;
    private int level;
    private String classId;
    private HClass hClass;
    private Traitline traitline;
    private Integer[] selectedTraits;
    private final Timestamp createdAt;
    private String lockedBy;
    private List<String> skills;
    private CompoundTag playerData;
    private boolean saveInventory = true;
    private boolean shouldSaveHotbarSeparately = false;

    private CharacterCastingManager castingManager;
    private boolean isInCastMode = false;
    private final ItemStack[] hotbar = new ItemStack[9];

    public HCharacter(UUID characterID, HPlayer hPlayer, int level, String classId, Timestamp createdAt, List<String> skills) {
        this.characterID = characterID;
        this.hPlayer = hPlayer;
        this.level = level;
        this.classId = classId;
        this.createdAt = createdAt;
        this.skills = skills;
        this.selectedTraits = new Integer[]{-1, -1, -1}; // Default traits
        if (classId != null) {
            hClass = plugin.getHClass(classId);
            if (hClass != null) {
                this.traitline = hClass.getStarterTraitline(); // Default traitline
            }
        }
    }

    public HCharacter(UUID characterID, HPlayer hPlayer, int level, String classId, Timestamp createdAt, String lockedBy, List<String> skills, String traitlineId, Integer[] traits) {
        this.characterID = characterID;
        this.hPlayer = hPlayer;
        this.level = level;
        this.classId = classId;
        this.createdAt = createdAt;
        this.lockedBy = lockedBy;
        this.skills = skills;
        this.selectedTraits = (traits != null) ? traits : new Integer[]{-1, -1, -1};

        if (classId != null) {
            this.hClass = plugin.getHClass(classId);
            if (this.hClass == null) {
                Hecate.log("Class " + classId + " not found for character " + characterID);
            } else {
                if (traitlineId != null) {
                    this.traitline = plugin.getTraitline(traitlineId);
                }
                if (this.traitline == null) {
                    Hecate.log("Traitline " + traitlineId + " not found or null for character " + characterID + ", using class default.");
                    this.traitline = hClass.getStarterTraitline();
                    this.selectedTraits = new Integer[]{-1, -1, -1};
                }
            }
        } else {
            Hecate.log("Class ID is null for character " + characterID);
        }
    }

    /**
     * Saves the core character data (level, class, skills, traits) and the
     * current player NBT data to the database using the CharacterDao.
     * Replaces the old createOrUpdateCharacter.
     *
     * @return CompletableFuture indicating completion.
     */
    public CompletableFuture<Void> saveToDatabase() {
        byte[] playerDataBytes = serializePlayerDataToBlob(false);
        if (playerDataBytes == null) {
            Hecate.log("Failed to serialize player data for character " + characterID + ". Aborting saveToDatabase.");
            return CompletableFuture.failedFuture(new RuntimeException("Player data serialization failed"));
        }

        String skillsString = (skills != null) ? String.join(",", skills) : "";
        String currentTraitlineId = (traitline != null) ? traitline.getId() : null;
        Integer[] currentSelectedTraits = (selectedTraits != null) ? selectedTraits : new Integer[0];

        return dbManager.executeAsync(handle -> {
            CharacterDao dao = handle.attach(CharacterDao.class);
            int rows = dao.upsertCharacter(
                    this.characterID,
                    this.hPlayer.getPlayerId(),
                    this.level,
                    this.classId,
                    playerDataBytes,
                    skillsString,
                    currentTraitlineId,
                    currentSelectedTraits
            );
            if (rows == 0) {
                Hecate.log("Warning: upsertCharacter affected 0 rows for " + characterID);
            }
        }).thenRun(() -> {
            Hecate.log("Successfully saved core data and player NBT for character: " + characterID);
        }).exceptionally(ex -> {
            Hecate.log("Failed to save character " + characterID + " to database: " + ex.getMessage());
            ex.printStackTrace();
            return null;
        });
    }

    /**
     * Loads only the character's player NBT data from the database using CharacterDao.
     * Updates the internal 'playerData' field and applies it to the player.
     *
     * @return CompletableFuture indicating completion.
     */
    public CompletableFuture<Void> loadCharacterPlayerData() {
        return dbManager.queryAsync(handle -> {
                    CharacterDao dao = handle.attach(CharacterDao.class);
                    return dao.findCharacterPlayerData(characterID);
                }).thenAcceptAsync(optionalData -> {
                    if (optionalData.isPresent() && optionalData.get().length > 0) {
                        deserializePlayerDataFromBlob(optionalData.get());
                        Hecate.log("Loaded character player data from DB for: " + getCharacterID());
                    } else {
                        Hecate.log("No character player data found in DB for: " + getCharacterID() + ". Player might need initial setup.");
                        this.playerData = new CompoundTag();
                    }
                }, runnable -> Bukkit.getScheduler().runTask(plugin, runnable))
                .exceptionally(ex -> {
                    Hecate.log("Failed to load character player data for " + characterID + ": " + ex.getMessage());
                    ex.printStackTrace();
                    return null;
                });
    }

    /**
     * Saves only the character's player NBT data to the database using CharacterDao.
     *
     * @param castModeSwitch Indicates if this save is happening due to a cast mode switch.
     * @return CompletableFuture indicating completion.
     */
    public CompletableFuture<Void> saveCharacterPlayerData(boolean castModeSwitch) {
        if (!saveInventory && !castModeSwitch) { // Don't save if inventory saving is off AND it's not a cast mode switch
            Hecate.log("Skipping player data save for " + characterID + " (saveInventory=false, not castModeSwitch)");
            return CompletableFuture.completedFuture(null);
        }

        byte[] blobData = serializePlayerDataToBlob(castModeSwitch);
        if (blobData == null || blobData.length == 0) {
            Hecate.log("Skipping save of null or empty player data blob for " + characterID);
            return CompletableFuture.completedFuture(null);
        }

        return dbManager.executeAsync(handle -> {
            CharacterDao dao = handle.attach(CharacterDao.class);
            int rows = dao.updateCharacterPlayerData(characterID, blobData);
            if (rows == 0) {
                Hecate.log("Failed to update player data for character " + characterID + ": Character not found in DB (0 rows affected).");
            } else {
                Hecate.log("Saved player data blob for character " + characterID + " (" + rows + " rows affected).");
                Hecate.log("Location: " + hPlayer.getPlayer().getLocation());
            }
        }).exceptionally(ex -> {
            Hecate.log("Failed to save character player data for " + characterID + ": " + ex.getMessage());
            ex.printStackTrace();
            return null;
        });
    }

    public byte[] serializePlayerDataToBlob(boolean castModeSwitch) {
        Player bukkitPlayer = hPlayer.getPlayer();
        if (bukkitPlayer == null) {
            Hecate.log("Cannot serialize player data for character " + characterID + ": Player is null");
            return null;
        }
        CraftPlayer craftPlayer = (CraftPlayer) bukkitPlayer;
        net.minecraft.world.entity.player.Player nmsPlayer = craftPlayer.getHandle();

        try (ProblemReporter.ScopedCollector scopedCollector = new ProblemReporter.ScopedCollector(nmsPlayer.problemPath(), LogUtils.getLogger())) {
            TagValueOutput tag = TagValueOutput.createWithContext(scopedCollector, nmsPlayer.registryAccess());
            craftPlayer.getHandle().saveWithoutId(tag);

            tag.discard("Paper.OriginWorld");
            tag.discard("UUID");

            String serializedHotbar = "";
            if (shouldSaveHotbarSeparately || castModeSwitch) {
                for (int i = 0; i < 9; i++) {
                    ItemStack item = hotbar[i];
                    if (item == null || item.getType().isAir()) {
                        serializedHotbar += "empty;";
                    } else {
                        byte[] itemBytes = item.serializeAsBytes();
                        if (itemBytes != null && itemBytes.length > 0) {
                            String serialized = Base64.getEncoder().encodeToString(itemBytes);
                            serializedHotbar += serialized + ";";
                        } else {
                            serializedHotbar += "empty;";
                            Hecate.log("Failed to serialize hotbar item at index " + i + " for character " + characterID);
                        }
                    }
                }
                Hecate.log("Serialized hotbar separately for " + characterID + ": " + serializedHotbar.length() + " length.");
                shouldSaveHotbarSeparately = false;
            }

            if (!serializedHotbar.isEmpty()) {
                tag.putString("hotbar", serializedHotbar);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            NbtIo.writeCompressed(tag.buildResult(), outputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            Hecate.log("Failed to serialize player data for character " + characterID + " of player " + hPlayer.getPlayerId());
            e.printStackTrace();
            return null;
        }
    }

    private void deserializePlayerDataFromBlob(byte[] blob) {
        Player bukkitPlayer = hPlayer.getPlayer();
        if (bukkitPlayer == null) {
            Hecate.log("Cannot deserialize player data for character " + characterID + ": Player is null.");
            return;
        }
        if  (!bukkitPlayer.isConnected()) {
            Hecate.log("Cannot deserialize player data for character " + characterID + ": Player is offline.");
            return;
        }
        CraftPlayer craftPlayer = (CraftPlayer) bukkitPlayer;
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(blob);
            CompoundTag tag = NbtIo.readCompressed(inputStream, NbtAccounter.create(20 * 1024 * 1024));
            this.playerData = tag;
            inputStream.close();

            // Apply data on the main thread
            new BukkitRunnable() {
                @Override
                public void run() {
                    try {
                        ServerPlayer serverPlayer = craftPlayer.getHandle();
                        Level serverLevel = serverPlayer.level();

                        // Todo: Readd Leafs DataFixer here
                        boolean wasInCastMode = tag.contains("HecateHotbar");

                        // If they were in cast mode, DO NOT load Pos/Motion/Rotation from the saved tag.
                        // Use the player's CURRENT position to prevent teleport exploits/issues.
                        if (wasInCastMode) {
                            tag.remove("Pos");
                            tag.remove("Motion");
                            tag.remove("Rotation");
                            tag.remove("FallDistance"); // Don't restore old fall distance
                            Hecate.log("Player " + characterID + " was in cast mode, not restoring position/rotation.");
                        }
                        // Remove UUID to prevent issues
                        tag.remove("UUID");
                        try (ProblemReporter.ScopedCollector scopedCollector = new ProblemReporter.ScopedCollector(serverPlayer.problemPath(), LogUtils.getLogger())) {
                            ValueInput tagInput = TagValueInput.create(scopedCollector, serverPlayer.registryAccess(), tag);
                            serverPlayer.load(tagInput);
                        }

                        if (wasInCastMode) {
                            Hecate.log("Player " + characterID + " was in cast mode, restoring hotbar...");
                            ListTag loadedHotbarTag = tag.getList("HecateHotbar").get();
                            for (int i = 0; i < Math.min(9, loadedHotbarTag.size()); i++) {
                                String encodedItem = loadedHotbarTag.getString(i).get();
                                if (encodedItem.equals("empty")) {
                                    hotbar[i] = null;
                                    bukkitPlayer.getInventory().setItem(i, null);
                                } else {
                                    try {
                                        byte[] bytes = Base64.getDecoder().decode(encodedItem);
                                        if (bytes.length > 0) {
                                            hotbar[i] = ItemStack.deserializeBytes(bytes);
                                            bukkitPlayer.getInventory().setItem(i, hotbar[i]);
                                        } else {
                                            hotbar[i] = null;
                                            bukkitPlayer.getInventory().setItem(i, null);
                                        }
                                    } catch (IllegalArgumentException e) {
                                        Hecate.log("Failed to decode Base64 hotbar item at index " + i + " for " + characterID + ": " + e.getMessage());
                                        hotbar[i] = null;
                                        bukkitPlayer.getInventory().setItem(i, null);
                                    } catch (Exception e) { // Catch potential deserialization errors
                                        Hecate.log("Failed to deserialize hotbar item at index " + i + " for " + characterID + ": " + e.getMessage());
                                        hotbar[i] = null;
                                        bukkitPlayer.getInventory().setItem(i, null);
                                    }
                                }
                            }
                            tag.remove("HecateHotbar");
                            Hecate.log("Hotbar restoration attempt finished for " + characterID + ".");
                        }

                        serverPlayer.onUpdateAbilities();
                        serverPlayer.getServer().getPlayerList().broadcastAll(new net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket(serverPlayer.getId(), serverPlayer.getEntityData().getNonDefaultValues()));
                        serverPlayer.containerMenu.broadcastChanges();
                        serverPlayer.inventoryMenu.broadcastChanges();

                        serverPlayer.connection.send(new net.minecraft.network.protocol.game.ClientboundSetExperiencePacket(serverPlayer.experienceProgress, serverPlayer.totalExperience, serverPlayer.experienceLevel));

                        for (net.minecraft.world.effect.MobEffectInstance mobEffect : serverPlayer.getActiveEffects()) {
                            serverPlayer.connection.send(new net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket(serverPlayer.getId(), mobEffect, false));
                        }

                        serverPlayer.getServer().getPlayerList().sendAllPlayerInfo(serverPlayer);

                        serverPlayer.connection.send(new net.minecraft.network.protocol.game.ClientboundSetHealthPacket(serverPlayer.getHealth(), serverPlayer.getFoodData().getFoodLevel(), serverPlayer.getFoodData().getSaturationLevel()));


                        if (!wasInCastMode) {
                            serverPlayer.setDeltaMovement(Vec3.ZERO);
                            craftPlayer.teleport(CraftLocation.toBukkit(serverPlayer.position(), serverLevel.getWorld(), serverPlayer.getYRot(), serverPlayer.getXRot()).add(0, 2, 0));
                            //serverPlayer.connection.teleport(serverPlayer.getX(), yToTeleport + 2, serverPlayer.getZ(), serverPlayer.getYRot(), serverPlayer.getXRot(), PlayerTeleportEvent.TeleportCause.PLUGIN);
                        }

                        // Spellbook:
                        craftPlayer.getUnlockedSpells().clear();
                        craftPlayer.getPassiveSpells().clear(); // Not used anymore, I think
                        craftPlayer.getActiveSpells().clear(); // Same
                        for (String spellId : skills) {
                            if (spellId != null && !spellId.isEmpty()) {
                                SpellData sData = Bukkit.getServer().getSpellbookAPI().getLibrary().getSpellByID(spellId);
                                craftPlayer.getUnlockedSpells().add(sData);
                            }
                        }

                        Hecate.log("Successfully applied deserialized player data for character " + characterID);
                        Hecate.log("Location: " + craftPlayer.getLocation());

                    } catch (Exception e) {
                        Hecate.log("Critical error applying deserialized player data for character " + characterID + " on main thread:");
                        e.printStackTrace();
                    }
                }
            }.runTask(plugin);

        } catch (Exception e) {
            Hecate.log("Failed to deserialize player data blob for character " + characterID);
            e.printStackTrace();
        }
    }

    public void switchCastMode(CombatModeReason reason, boolean newMode) {
        if (castingManager == null) {
            castingManager = new CharacterCastingManager(this);
        }
        Player player = getPlayer();
        if (player == null || !player.isOnline()) return;

        if (newMode) {
            for (int i = 0; i < 9; i++) {
                hotbar[i] = player.getInventory().getItem(i);
            }
            ItemStack weapon = player.getInventory().getItemInMainHand();
            shouldSaveHotbarSeparately = true;

            saveCharacterPlayerData(true)
                    .thenRunAsync(() -> {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            try {
                                if (player.isOnline()) { // Double-check player online status
                                    castingManager.switchCastMode(reason, true, weapon);
                                    isInCastMode = true;
                                    MessageUtil.sendMessage(player, "Entered cast mode");
                                } else {
                                    Hecate.log("Player logged off before cast mode switch could complete for " + characterID);
                                }
                            } catch (Exception e) {
                                Hecate.log("Error switching to cast mode for " + characterID);
                                e.printStackTrace();
                            }
                        });
                    }, runnable -> Bukkit.getScheduler().runTask(plugin, runnable)); // Ensure callback is on main thread
        } else {
            if (player.isOnline()) {
                castingManager.switchCastMode(reason, false, null);
                isInCastMode = false;
                MessageUtil.sendMessage(player, "Left cast mode");
                for (int i = 0; i < 9; i++) {
                    player.getInventory().setItem(i, hotbar[i]);
                }
                shouldSaveHotbarSeparately = false;
                saveCharacterPlayerData(false);
            }
        }
    }

    public void setScaledPvP(boolean scaledPvP) {
        if (castingManager != null) {
            castingManager.setScaledPvPMode(scaledPvP);
            Hecate.log("Scaled PvP mode set to " + scaledPvP + " for character " + characterID);
        }
    }


    public boolean isInCastMode() { return isInCastMode; }
    public CharacterCastingManager getCastingManager() { return castingManager; }
    public UUID getCharacterID() { return characterID; }
    public HPlayer getHPlayer() { return hPlayer; }

    public int getLevel() {
        try {
            return LevelUtil.getCharacterLevel(this).get();
        } catch (InterruptedException | ExecutionException e) {
            return 0; // Handle potential errors gracefully
        }
    }
    public String getClassId() { return classId; }
    public HClass getHClass() { return hClass; }
    public void setHClass(HClass hClass) { this.hClass = hClass; }
    public Traitline getTraitline() { return traitline; }

    public void setTraitline(Traitline traitline) {
        getPlayer().getActiveTraits().clear();
        this.traitline = traitline;
    }

    public Timestamp getCreatedAt() { return createdAt; }
    public String getLockedBy() { return lockedBy; }
    public void setLockedBy(String lockedBy) { this.lockedBy = lockedBy; }
    public List<String> getSkills() { return skills; }
    public Integer[] getSelectedTraits() { return selectedTraits; }
    public Player getPlayer() { return hPlayer.getPlayer(); }
    public CompoundTag getPlayerDataNBT() { return playerData; }

    public void setLevel(int level) { this.level = level; }
    public void setSelectedTraits(Integer[] selectedTraits) { this.selectedTraits = selectedTraits; }


}