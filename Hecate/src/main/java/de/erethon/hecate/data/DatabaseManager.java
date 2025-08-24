package de.erethon.hecate.data;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.database.BedrockDBConnection;
import de.erethon.bedrock.database.EDatabaseManager;
import de.erethon.hecate.Hecate;
import de.erethon.hecate.data.dao.CharacterDao;
import de.erethon.hecate.data.dao.PlayerDao;
import de.erethon.hecate.events.PlayerSelectedCharacterEvent;
import de.erethon.hecate.progression.LevelUtil;
import de.erethon.papyrus.events.PlayerDataRequestEvent;
import de.erethon.papyrus.events.PlayerDataSaveEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jdbi.v3.core.Handle;

import java.io.ByteArrayInputStream;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

public class DatabaseManager extends EDatabaseManager implements Listener {

    private final Map<UUID, HPlayer> uuidToHPlayerMap = new ConcurrentHashMap<>();

    private final PlayerDao playerDao;
    private final CharacterDao characterDao;

    public DatabaseManager(BedrockDBConnection connection) {
        super(connection, new ThreadPoolExecutor(2, 4, 60L, java.util.concurrent.TimeUnit.SECONDS, new java.util.concurrent.LinkedBlockingQueue<>()));

        this.playerDao = getDao(PlayerDao.class);
        this.characterDao = getDao(CharacterDao.class);

        Bukkit.getPluginManager().registerEvents(this, Hecate.getInstance());
    }

    @Override
    protected CompletableFuture<Void> initializeSchema() {
        String createCharactersTable = "CREATE TABLE IF NOT EXISTS Characters (" +
                "character_id UUID PRIMARY KEY," +
                "player_id UUID," +
                "level INT," +
                "class_id VARCHAR(255)," +
                "playerdata BYTEA," +
                "created_at TIMESTAMP," +
                "locked_by TEXT," +
                "skills TEXT," +
                "traitline VARCHAR(255)," +
                "selected_traits INTEGER[]," +
                "UNIQUE (character_id))";

        String createPlayersTable = "CREATE TABLE IF NOT EXISTS Players (" +
                "player_id UUID PRIMARY KEY," +
                "last_online TIMESTAMP," +
                "last_character UUID)";

        String addForeignKeyToPlayers = "DO $$ BEGIN " +
                "IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_last_character' AND conrelid = 'players'::regclass) THEN " +
                "ALTER TABLE Players ADD CONSTRAINT fk_last_character FOREIGN KEY (last_character) REFERENCES Characters(character_id) ON DELETE SET NULL; " +
                "END IF; " +
                "END $$;";

        String addForeignKeyToCharacters = "DO $$ BEGIN " +
                "IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_player_id' AND conrelid = 'characters'::regclass) THEN " +
                "ALTER TABLE Characters ADD CONSTRAINT fk_player_id FOREIGN KEY (player_id) REFERENCES Players(player_id) ON DELETE CASCADE; " +
                "END IF; " +
                "END $$;";

        List<String> alterTableStatements = Arrays.asList(
                "ALTER TABLE Characters ADD COLUMN IF NOT EXISTS traitline VARCHAR(255)",
                "ALTER TABLE Characters ADD COLUMN IF NOT EXISTS selected_traits INTEGER[]"
        );

        return CompletableFuture.runAsync(() -> { // Use our own future here as the normal one is not ready yet
            jdbi.useHandle(handle -> {
                Hecate.log("Schema setup: Starting...");
                try {
                    Hecate.log("Schema setup: Executing createPlayersTable...");
                    handle.execute(createPlayersTable);
                    Hecate.log("Schema setup: Finished createPlayersTable.");

                    Hecate.log("Schema setup: Executing createCharactersTable...");
                    handle.execute(createCharactersTable);
                    Hecate.log("Schema setup: Finished createCharactersTable.");

                    Hecate.log("Schema setup: Executing alter table statements...");
                    alterTableStatements.forEach(statement -> {
                        Hecate.log("Schema setup: Altering -> " + statement);
                        handle.execute(statement);
                    });
                    Hecate.log("Schema setup: Finished alter table statements.");

                    Hecate.log("Schema setup: Executing addForeignKeyToCharacters...");
                    handle.execute(addForeignKeyToCharacters);
                    Hecate.log("Schema setup: Finished addForeignKeyToCharacters.");

                    Hecate.log("Schema setup: Executing addForeignKeyToPlayers...");
                    handle.execute(addForeignKeyToPlayers);
                    Hecate.log("Schema setup: Finished addForeignKeyToPlayers.");

                    Hecate.log("Schema setup: All statements executed successfully.");
                } catch (Exception e) {
                    Hecate.log("!!! Critical error with schema setup async task !!!");
                    e.printStackTrace();
                    throw new RuntimeException("Error during async schema execution", e);
                }
            });
        }, asyncExecutor);
    }

    @Override
    protected void registerCustomMappers() {
    }


    public CompletableFuture<Boolean> createOrUpdateCharacter(HCharacter character) {
        byte[] playerData = character.serializePlayerDataToBlob(false); // Assume this method exists
        String skillsString = String.join(",", character.getSkills());
        String traitlineId = character.getTraitline() != null ? character.getTraitline().getId() : null;
        Integer[] selectedTraitsArray = character.getSelectedTraits();

        return queryAsync(handle -> characterDao.upsertCharacter(
                character.getCharacterID(),
                character.getHPlayer().getPlayerId(),
                character.getLevel(),
                character.getClassId(),
                playerData,
                skillsString,
                traitlineId,
                selectedTraitsArray
        ) > 0)
        .exceptionally(ex -> {
            Hecate.log("Error saving character " + character.getCharacterID() + " for player " + character.getHPlayer().getPlayerId() + ": " + ex.getMessage());
            ex.printStackTrace();
            return false;
        });
    }

    private CompletableFuture<HPlayer> loadPlayerData(UUID playerId) {
        return queryAsync(handle -> {
            // Check if player exists first
            if (!playerDao.playerExists(playerId)) {
                Hecate.log("Player " + playerId + " not found in DB. Will create on demand.");
                return null;
            }

            HPlayer hPlayer = new HPlayer(playerId);

            List<CharacterDao.FlatData> charDataList = characterDao.findCharactersByPlayerId(playerId);
            List<HCharacter> characters = new ArrayList<>();
            for (CharacterDao.FlatData flatData : charDataList) {
                try {
                    characters.add(mapFlatDataToHCharacter(flatData, hPlayer, handle)); // Pass handle for array conversion
                    Hecate.log("Loaded character " + flatData.getCharacterId() + " for player " + playerId);
                } catch (Exception e) {
                    Hecate.log("Error mapping character data for " + flatData.getCharacterId() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
            hPlayer.setCharacters(characters);
            hPlayer.setLastCharacter(playerDao.findLastCharacterId(playerId).orElse(null));
            Hecate.log("Loaded player data and " + characters.size() + " characters for " + playerId);
            return hPlayer;
        }).exceptionally(ex -> {
            Hecate.log("Error loading player data for " + playerId + ": " + ex.getMessage());
            ex.printStackTrace();
            return null;
        });
    }

    private HCharacter mapFlatDataToHCharacter(CharacterDao.FlatData flatData, HPlayer hPlayer, Handle handle) throws SQLException {
        List<String> skills = new ArrayList<>();
        if (flatData.getSkills() != null && !flatData.getSkills().isEmpty()) {
            skills.addAll(Arrays.asList(flatData.getSkills().split(",")));
        }

        Integer[] selectedTraits = flatData.getSelectedTraits();

        return new HCharacter(
                flatData.getCharacterId(),
                hPlayer,
                flatData.getLevel(),
                flatData.getClassId(),
                flatData.getCreatedAt(),
                flatData.getLockedBy(),
                skills,
                flatData.getTraitline(),
                selectedTraits
        );
    }

    public CompletableFuture<Boolean> saveCharacterPlayerData(UUID characterId, byte[] playerData) {
        return executeAsync(handle -> characterDao.updateCharacterPlayerData(characterId, playerData))
                .thenApply(v -> true)
                .exceptionally(ex -> {
                    Hecate.log("Error saving playerdata for character " + characterId + ": " + ex.getMessage());
                    ex.printStackTrace();
                    return false;
                });
    }

    public CompletableFuture<byte[]> loadCharacterPlayerData(UUID characterId) {
        return queryAsync(handle -> characterDao.findCharacterPlayerData(characterId))
                .thenApply(optionalData -> optionalData.orElse(null))
                .exceptionally(ex -> {
                    Hecate.log("Error loading playerdata for character " + characterId + ": " + ex.getMessage());
                    ex.printStackTrace();
                    return null;
                });
    }

    public CompoundTag getPlayerDataTag(UUID characterId) {
        byte[] data = loadCharacterPlayerData(characterId).join();
        if (data == null) {
            Hecate.log("No player data found for character " + characterId);
            return null;
        }
        CompoundTag tag = new CompoundTag();
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
            tag = NbtIo.readCompressed(inputStream, NbtAccounter.create(20 * 1024 * 1024));
            inputStream.close();
        } catch (Exception e) {
            Hecate.log("Error converting player data to CompoundTag for character " + characterId + ": " + e.getMessage());
            e.printStackTrace();
        }
        return tag;
    }

    public HPlayer getHPlayer(Player player) {
        return uuidToHPlayerMap.get(player.getUniqueId());
    }

    public HPlayer getHPlayer(UUID uuid) {
        return uuidToHPlayerMap.get(uuid);
    }

    public HCharacter getCurrentCharacter(Player player) {
        HPlayer hPlayer = getHPlayer(player);
        return (hPlayer != null) ? hPlayer.getSelectedCharacter() : null;
    }

    public Collection<HPlayer> getLoadedPlayers() {
        return uuidToHPlayerMap.values();
    }

    public List<HCharacter> getCachedCharactersForPlayer(UUID playerId) {
        HPlayer hPlayer = uuidToHPlayerMap.get(playerId);
        return (hPlayer != null) ? hPlayer.getCharacters() : new ArrayList<>();
    }

    public CompletableFuture<List<HCharacter>> loadCharactersForPlayer(UUID playerId) {
        HPlayer tempHPlayer = uuidToHPlayerMap.computeIfAbsent(playerId, HPlayer::new);
        return queryAsync(handle ->
                characterDao.findCharactersByPlayerId(playerId)
                        .stream()
                        .map(flatData -> {
                            try {
                                return mapFlatDataToHCharacter(flatData, tempHPlayer, handle);
                            } catch (Exception e) {
                                Hecate.log("Error mapping character data for player " + playerId + ": " + e.getMessage());
                                e.printStackTrace();
                                return null;
                            }
                        })
                        .filter(java.util.Objects::nonNull)
                        .collect(Collectors.toList())
        );
    }

    public CompletableFuture<Boolean> selectAndUseCharacter(UUID characterId, String lockOwner) {
        return lockCharacter(characterId, lockOwner)
                .thenCompose(lockSuccess -> {
                    if (lockSuccess) {
                        return queryAsync(handle -> {
                            CharacterDao dao = handle.attach(CharacterDao.class);
                            return dao.findCharacterById(characterId).isPresent();
                        })
                                .thenApply(found -> {
                                    if (found) {
                                        Hecate.log("Character " + characterId + " confirmed locked by " + lockOwner + " and exists.");
                                        return true;
                                    } else {
                                        Hecate.log("Character " + characterId + " was locked by " + lockOwner + " but NOT found! Releasing lock.");
                                        unlockCharacter(characterId);
                                        return false;
                                    }
                                });
                    } else {
                        return CompletableFuture.completedFuture(false);
                    }
                })
                .exceptionally(ex -> {
                    Hecate.log("Error during select/lock process for character " + characterId + ": " + ex.getMessage());
                    ex.printStackTrace();
                    checkIfLockedBy(characterId, lockOwner).thenAccept(isLockedByMe -> {
                        if(isLockedByMe) {
                            Hecate.log("Unlocking character " + characterId + " due to exception during selectAndUseCharacter.");
                            unlockCharacter(characterId);
                        }
                    });
                    return false;
                });
    }

    /** Helper to check if a character is currently locked by a specific owner */
    private CompletableFuture<Boolean> checkIfLockedBy(UUID characterId, String expectedOwner) {
        return queryAsync(handle -> handle.attach(CharacterDao.class).getLockOwner(characterId))
                .thenApply(optionalOwner -> optionalOwner.map(owner -> owner.equals(expectedOwner)).orElse(false))
                .exceptionally(ex -> {
                    Hecate.log("Failed to check lock status for " + characterId + ": " + ex.getMessage());
                    return false;
                });
    }

    public CompletableFuture<Void> savePlayerData(HPlayer hPlayer) {
        if (hPlayer == null) {
            return CompletableFuture.completedFuture(null);
        }
        UUID lastCharId = hPlayer.getLastCharacter();
        Timestamp now = Timestamp.from(Instant.now());

        return executeAsync(handle -> playerDao.upsertPlayer(hPlayer.getPlayerId(), now, lastCharId))
                .exceptionally(ex -> {
                    Hecate.log("Error saving player data for " + hPlayer.getPlayerId() + ": " + ex.getMessage());
                    ex.printStackTrace();
                    return null;
                }).thenApply(v -> null);
    }


    public CompletableFuture<Boolean> lockCharacter(UUID characterId, String lockOwner) {
        return queryAsync(handle -> {
            CharacterDao dao = handle.attach(CharacterDao.class);
            return dao.lockCharacter(characterId, lockOwner);
        })
                .thenApply(rowsAffected -> {
                    boolean success = rowsAffected > 0;
                    if (success) {
                        Hecate.log("Character " + characterId + " locked successfully by " + lockOwner);
                    } else {
                        Hecate.log("Failed to acquire lock for character " + characterId + " by " + lockOwner + " (already locked or not found).");
                    }
                    return success;
                })
                .exceptionally(ex -> {
                    Hecate.log("Error locking character " + characterId + " by " + lockOwner + ": " + ex.getMessage());
                    ex.printStackTrace();
                    return false;
                });
    }

    public CompletableFuture<Void> unlockCharacter(UUID characterId) {
        return executeAsync(handle -> {
            CharacterDao dao = handle.attach(CharacterDao.class);
            int rowsAffected = dao.unlockCharacter(characterId);
            if (rowsAffected == 0) {
                Hecate.log("Unlock command for character " + characterId + " affected 0 rows (was likely already unlocked or not found).");
            } else {
                Hecate.log("Character " + characterId + " unlocked successfully.");
            }
        })
                // No .thenApply needed here, executeAsync already returns CompletableFuture<Void>
                .exceptionally(ex -> {
                    Hecate.log("Error unlocking character " + characterId + ": " + ex.getMessage());
                    ex.printStackTrace();
                    return null;
                });
    }

    public CompletableFuture<HCharacter> getCharacterData(UUID characterId) {
        return queryAsync(handle -> characterDao.findCharacterById(characterId))
                .thenApply(optionalFlatData -> {
                    if (optionalFlatData.isEmpty()) {
                        Hecate.log("No character found with ID " + characterId);
                        return null;
                    }
                    CharacterDao.FlatData flatData = optionalFlatData.get();
                    HPlayer hPlayer = uuidToHPlayerMap.computeIfAbsent(flatData.getPlayerId(), HPlayer::new);
                    try(Handle handle = jdbi.open()) { // Need a handle for array mapping
                        return mapFlatDataToHCharacter(flatData, hPlayer, handle);
                    } catch (Exception e) {
                        Hecate.log("Error mapping character data for ID " + characterId + ": " + e.getMessage());
                        e.printStackTrace();
                        return null;
                    }
                })
                .exceptionally(ex -> {
                    Hecate.log("Error retrieving character data for ID " + characterId + ": " + ex.getMessage());
                    ex.printStackTrace();
                    return null;
                });
    }

    // --- Event Handlers ---

    @EventHandler
    private void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        HPlayer hPlayer = uuidToHPlayerMap.get(uuid);

        if (hPlayer != null) {
            hPlayer.setLastSeen(Timestamp.from(Instant.now()));
            HCharacter character = hPlayer.getSelectedCharacter();
            CompletableFuture<Void> saveFuture = savePlayerData(hPlayer);

            if (character != null) {
                saveFuture = saveFuture.thenCompose(v -> createOrUpdateCharacter(character))
                        .thenCompose(v -> unlockCharacter(character.getCharacterID()));
            }

            saveFuture.whenComplete((result, throwable) -> {
                if (throwable != null) {
                    Hecate.log("Error during quit save/unlock for " + uuid + ": " + throwable.getMessage());
                    throwable.printStackTrace();
                } else {
                    Hecate.log("Saved and unlocked data for player " + uuid + " on quit.");
                }
                uuidToHPlayerMap.remove(uuid);
                Hecate.log("Removed player " + uuid + " from cache.");
            });
        } else {
            Hecate.log("Player " + uuid + " not found in cache during quit event.");
        }
    }

    @EventHandler
    private void onLogin(AsyncPlayerPreLoginEvent event) {
        UUID uuid = event.getUniqueId();
        Hecate.log("AsyncPlayerPreLoginEvent for " + uuid);

        try {
            HPlayer hPlayer = loadPlayerData(uuid).join(); // Block here, we need to wait until the data is loaded to proceed.
            if (hPlayer == null) {
                Hecate.log("No data found for " + uuid + ". Creating new entry.");
                HPlayer newHPlayer = new HPlayer(uuid);
                uuidToHPlayerMap.put(uuid, newHPlayer);
                Hecate.log("Created shell HPlayer for new player " + uuid);

            } else {
                // Player data loaded successfully, cache it
                uuidToHPlayerMap.put(uuid, hPlayer);
                Hecate.log("Cached loaded HPlayer for " + uuid);
            }
        } catch (Exception e) {
            Hecate.log("Error loading HPlayer for " + uuid + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    @EventHandler
    private void onLogin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        HPlayer hPlayer = uuidToHPlayerMap.get(uuid);

        if (hPlayer == null) {
            // This *shouldn't* happen if AsyncPlayerPreLoginEvent worked correctly
            Hecate.log("HPlayer not found in cache for " + uuid + " during PlayerJoinEvent! Creating new.");
            hPlayer = new HPlayer(uuid);
            uuidToHPlayerMap.put(uuid, hPlayer);
            savePlayerData(hPlayer);
        }

        hPlayer.setPlayer(player);
        Hecate.log("Associated Player object with HPlayer for " + uuid);

        if (hPlayer.getLastCharacter() != null) {
            CompletableFuture<HCharacter> characterFuture = getCharacterData(hPlayer.getLastCharacter());
            HPlayer finalHPlayer = hPlayer;
            Hecate.log("Found last character ID " + hPlayer.getLastCharacter() + " for player " + uuid);
            characterFuture.thenAccept(character -> {
                if (character != null) {
                    try {
                        finalHPlayer.setSelectedCharacter(character, false);
                        BukkitRunnable mainTask = new BukkitRunnable() {
                            @Override
                            public void run() {
                                PlayerSelectedCharacterEvent event = new PlayerSelectedCharacterEvent(finalHPlayer, character, false);
                                Bukkit.getPluginManager().callEvent(event);
                                player.removePotionEffect(PotionEffectType.BLINDNESS);
                                Title title = Title.title(Component.empty(), Component.empty());
                                player.showTitle(title);
                                LevelUtil.displayCharLevel(finalHPlayer.getPlayer());
                            }
                        };
                        mainTask.runTaskLater(Hecate.getInstance(), 20);
                        MessageUtil.sendMessage(finalHPlayer.getPlayer(), "<green>Switched to last selected character.");
                    } catch (Exception e) {
                        MessageUtil.sendMessage(finalHPlayer.getPlayer(), "<red>Error switching character: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    // This handles the vanilla auto-save of player data.
    @EventHandler
    private void onPlayerDataSave(PlayerDataSaveEvent event) {
        HPlayer hPlayer = uuidToHPlayerMap.get(event.getPlayer().getUniqueId());
        if (hPlayer != null) {
            Hecate.log("PlayerDataSaveEvent triggered for " + hPlayer.getPlayerId());
            savePlayerData(hPlayer);
            if (hPlayer.getSelectedCharacter() != null) {
                createOrUpdateCharacter(hPlayer.getSelectedCharacter());
            }
        }
    }

    // This mostly handles plugins requesting player data via the API, for example, for lastSeen values.
    @EventHandler
    private void onPlayerDataRequest(PlayerDataRequestEvent event) {
        UUID uuid = UUID.fromString(event.getUUID());
        HPlayer hPlayer = getHPlayer(uuid);
        UUID lastCharacterId = hPlayer != null ? hPlayer.getLastCharacter() : null;
        Hecate.log("PlayerDataRequestEvent triggered for " + uuid);
        if (lastCharacterId != null) {
            CompoundTag playerDataTag = getPlayerDataTag(lastCharacterId);
            if (playerDataTag != null) {
                event.setData(Optional.of(playerDataTag));
            } else {
                event.setData(Optional.empty());
            }
        }
    }

    // --- Util methods ---

    public long getLastSeen(UUID playerId) {
        HPlayer hPlayer = getHPlayer(playerId);
        if (hPlayer != null) {
            if (hPlayer.getLastSeen() == null) {
                return 0;
            }
            return hPlayer.getLastSeen().getTime();
        }
        return 0;
    }

    // --- Shutdown handling ---

    @Override
    public void close() {
        Hecate.log("DatabaseManager is closing. Saving all player data...");
        int players = getLoadedPlayers().size();
        int saved = 0;
        for (HPlayer hPlayer : getLoadedPlayers()) {
            try {
                savePlayerData(hPlayer).join();
                saved++;
            } catch (Exception e) {
                Hecate.log("Error saving player " + hPlayer.getPlayerId() + " data during shutdown: " + e.getMessage());
                e.printStackTrace();
            }
        }
        Hecate.log("Saved " + saved + "/" + players + " players before shutdown.");
        super.close();
    }
}