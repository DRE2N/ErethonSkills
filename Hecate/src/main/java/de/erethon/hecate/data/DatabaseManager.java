package de.erethon.hecate.data;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.database.BedrockDBConnection;
import de.erethon.bedrock.database.EDatabaseManager;
import de.erethon.bedrock.jdbi.v3.core.Handle;
import de.erethon.hecate.Hecate;
import de.erethon.hecate.data.dao.CharacterDao;
import de.erethon.hecate.data.dao.PlayerDao;
import de.erethon.papyrus.events.PlayerDataSaveEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class DatabaseManager extends EDatabaseManager implements Listener {

    private final Map<UUID, HPlayer> uuidToHPlayerMap = new ConcurrentHashMap<>();

    private final PlayerDao playerDao;
    private final CharacterDao characterDao;

    public DatabaseManager(BedrockDBConnection connection) {
        super(connection, runnable -> Bukkit.getScheduler().runTaskAsynchronously(Hecate.getInstance(), runnable));

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

        String createCompletedQuestsTable = "CREATE TABLE IF NOT EXISTS Completed_Quests (" +
                "character_id UUID," +
                "quest_id VARCHAR(255)," +
                "completed_at TIMESTAMP," +
                "PRIMARY KEY (character_id, quest_id))";

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

        String addForeignKeyToCompletedQuests = "DO $$ BEGIN " +
                "IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_character_id_quests' AND conrelid = 'completed_quests'::regclass) THEN " +
                "ALTER TABLE Completed_Quests ADD CONSTRAINT fk_character_id_quests FOREIGN KEY (character_id) REFERENCES Characters(character_id) ON DELETE CASCADE; " +
                "END IF; " +
                "END $$;";

        List<String> alterTableStatements = Arrays.asList(
                "ALTER TABLE Characters ADD COLUMN IF NOT EXISTS traitline VARCHAR(255)",
                "ALTER TABLE Characters ADD COLUMN IF NOT EXISTS selected_traits INTEGER[]"
        );

        return CompletableFuture.runAsync(() -> { // Use our own future here as the normal one is not ready yet
            jdbi.useHandle(handle -> {
                MessageUtil.log("Schema setup: Starting...");
                try {
                    MessageUtil.log("Schema setup: Executing createPlayersTable...");
                    handle.execute(createPlayersTable);
                    MessageUtil.log("Schema setup: Finished createPlayersTable.");

                    MessageUtil.log("Schema setup: Executing createCharactersTable...");
                    handle.execute(createCharactersTable);
                    MessageUtil.log("Schema setup: Finished createCharactersTable.");

                    MessageUtil.log("Schema setup: Executing createCompletedQuestsTable...");
                    handle.execute(createCompletedQuestsTable);
                    MessageUtil.log("Schema setup: Finished createCompletedQuestsTable.");

                    MessageUtil.log("Schema setup: Executing alter table statements...");
                    alterTableStatements.forEach(statement -> {
                        MessageUtil.log("Schema setup: Altering -> " + statement);
                        handle.execute(statement);
                    });
                    MessageUtil.log("Schema setup: Finished alter table statements.");

                    MessageUtil.log("Schema setup: Executing addForeignKeyToCharacters...");
                    handle.execute(addForeignKeyToCharacters);
                    MessageUtil.log("Schema setup: Finished addForeignKeyToCharacters.");

                    MessageUtil.log("Schema setup: Executing addForeignKeyToPlayers...");
                    handle.execute(addForeignKeyToPlayers);
                    MessageUtil.log("Schema setup: Finished addForeignKeyToPlayers.");

                    MessageUtil.log("Schema setup: Executing addForeignKeyToCompletedQuests...");
                    handle.execute(addForeignKeyToCompletedQuests);
                    MessageUtil.log("Schema setup: Finished addForeignKeyToCompletedQuests.");

                    MessageUtil.log("Schema setup: All statements executed successfully.");
                } catch (Exception e) {
                    MessageUtil.log("!!! Critical error INSIDE schema setup async task !!!");
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

        return queryAsync(handle -> {
            return characterDao.upsertCharacter(
                    character.getCharacterID(),
                    character.getHPlayer().getPlayerId(),
                    character.getLevel(),
                    character.getClassId(),
                    playerData,
                    skillsString,
                    traitlineId,
                    selectedTraitsArray
            ) > 0;
        })
        .exceptionally(ex -> {
            MessageUtil.log("Error saving character " + character.getCharacterID() + " for player " + character.getHPlayer().getPlayerId() + ": " + ex.getMessage());
            ex.printStackTrace();
            return false;
        });
    }

    private CompletableFuture<HPlayer> loadPlayerData(UUID playerId) {
        return queryAsync(handle -> {
            // Check if player exists first
            if (!playerDao.playerExists(playerId)) {
                MessageUtil.log("Player " + playerId + " not found in DB. Will create on demand.");
                return null;
            }

            HPlayer hPlayer = new HPlayer(playerId);

            List<CharacterDao.FlatData> charDataList = characterDao.findCharactersByPlayerId(playerId);
            List<HCharacter> characters = new ArrayList<>();
            for (CharacterDao.FlatData flatData : charDataList) {
                try {
                    characters.add(mapFlatDataToHCharacter(flatData, hPlayer, handle)); // Pass handle for array conversion
                    MessageUtil.log("Loaded character " + flatData.getCharacterId() + " for player " + playerId);
                } catch (Exception e) {
                    MessageUtil.log("Error mapping character data for " + flatData.getCharacterId() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
            hPlayer.setCharacters(characters);
            MessageUtil.log("Loaded player data and " + characters.size() + " characters for " + playerId);
            return hPlayer;
        }).exceptionally(ex -> {
            MessageUtil.log("Error loading player data for " + playerId + ": " + ex.getMessage());
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
                    MessageUtil.log("Error saving playerdata for character " + characterId + ": " + ex.getMessage());
                    ex.printStackTrace();
                    return false;
                });
    }

    public CompletableFuture<byte[]> loadCharacterPlayerData(UUID characterId) {
        return queryAsync(handle -> characterDao.findCharacterPlayerData(characterId))
                .thenApply(optionalData -> optionalData.orElse(null))
                .exceptionally(ex -> {
                    MessageUtil.log("Error loading playerdata for character " + characterId + ": " + ex.getMessage());
                    ex.printStackTrace();
                    return null;
                });
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
                                MessageUtil.log("Error mapping character data for player " + playerId + ": " + e.getMessage());
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
                                        MessageUtil.log("Character " + characterId + " confirmed locked by " + lockOwner + " and exists.");
                                        return true;
                                    } else {
                                        MessageUtil.log("Character " + characterId + " was locked by " + lockOwner + " but NOT found! Releasing lock.");
                                        unlockCharacter(characterId);
                                        return false;
                                    }
                                });
                    } else {
                        return CompletableFuture.completedFuture(false);
                    }
                })
                .exceptionally(ex -> {
                    MessageUtil.log("Error during select/lock process for character " + characterId + ": " + ex.getMessage());
                    ex.printStackTrace();
                    checkIfLockedBy(characterId, lockOwner).thenAccept(isLockedByMe -> {
                        if(isLockedByMe) {
                            MessageUtil.log("Unlocking character " + characterId + " due to exception during selectAndUseCharacter.");
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
                    MessageUtil.log("Failed to check lock status for " + characterId + ": " + ex.getMessage());
                    return false;
                });
    }

    public CompletableFuture<Void> savePlayerData(HPlayer hPlayer) {
        if (hPlayer == null) {
            return CompletableFuture.completedFuture(null);
        }
        UUID lastCharId = (hPlayer.getSelectedCharacter() != null) ? hPlayer.getSelectedCharacter().getCharacterID() : null;
        Timestamp now = Timestamp.from(Instant.now());

        return executeAsync(handle -> playerDao.upsertPlayer(hPlayer.getPlayerId(), now, lastCharId))
                .exceptionally(ex -> {
                    MessageUtil.log("Error saving player data for " + hPlayer.getPlayerId() + ": " + ex.getMessage());
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
                        MessageUtil.log("Character " + characterId + " locked successfully by " + lockOwner);
                    } else {
                        MessageUtil.log("Failed to acquire lock for character " + characterId + " by " + lockOwner + " (already locked or not found).");
                    }
                    return success;
                })
                .exceptionally(ex -> {
                    MessageUtil.log("Error locking character " + characterId + " by " + lockOwner + ": " + ex.getMessage());
                    ex.printStackTrace();
                    return false;
                });
    }

    public CompletableFuture<Void> unlockCharacter(UUID characterId) {
        return executeAsync(handle -> {
            CharacterDao dao = handle.attach(CharacterDao.class);
            int rowsAffected = dao.unlockCharacter(characterId);
            if (rowsAffected == 0) {
                MessageUtil.log("Unlock command for character " + characterId + " affected 0 rows (was likely already unlocked or not found).");
            } else {
                MessageUtil.log("Character " + characterId + " unlocked successfully.");
            }
        })
                // No .thenApply needed here, executeAsync already returns CompletableFuture<Void>
                .exceptionally(ex -> {
                    MessageUtil.log("Error unlocking character " + characterId + ": " + ex.getMessage());
                    ex.printStackTrace();
                    return null;
                });
    }

    public CompletableFuture<HCharacter> getCharacterData(UUID characterId) {
        return queryAsync(handle -> characterDao.findCharacterById(characterId))
                .thenApply(optionalFlatData -> {
                    if (optionalFlatData.isEmpty()) {
                        MessageUtil.log("No character found with ID " + characterId);
                        return null;
                    }
                    CharacterDao.FlatData flatData = optionalFlatData.get();
                    HPlayer hPlayer = uuidToHPlayerMap.computeIfAbsent(flatData.getPlayerId(), HPlayer::new);
                    try(Handle handle = jdbi.open()) { // Need a handle for array mapping
                        return mapFlatDataToHCharacter(flatData, hPlayer, handle);
                    } catch (Exception e) {
                        MessageUtil.log("Error mapping character data for ID " + characterId + ": " + e.getMessage());
                        e.printStackTrace();
                        return null;
                    }
                })
                .exceptionally(ex -> {
                    MessageUtil.log("Error retrieving character data for ID " + characterId + ": " + ex.getMessage());
                    ex.printStackTrace();
                    return null;
                });
    }

    // --- Event Handlers ---

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        HPlayer hPlayer = uuidToHPlayerMap.get(uuid);

        if (hPlayer != null) {
            HCharacter character = hPlayer.getSelectedCharacter();
            CompletableFuture<Void> saveFuture = savePlayerData(hPlayer);

            if (character != null) {
                saveFuture = saveFuture.thenCompose(v -> createOrUpdateCharacter(character))
                        .thenCompose(v -> unlockCharacter(character.getCharacterID()));
            }

            saveFuture.whenComplete((result, throwable) -> {
                if (throwable != null) {
                    MessageUtil.log("Error during quit save/unlock for " + uuid + ": " + throwable.getMessage());
                    throwable.printStackTrace();
                } else {
                    MessageUtil.log("Saved and unlocked data for player " + uuid + " on quit.");
                }
                uuidToHPlayerMap.remove(uuid);
                MessageUtil.log("Removed player " + uuid + " from cache.");
            });
        } else {
            MessageUtil.log("Player " + uuid + " not found in cache during quit event.");
        }
    }

    @EventHandler
    public void onLogin(AsyncPlayerPreLoginEvent event) {
        UUID uuid = event.getUniqueId();
        MessageUtil.log("AsyncPlayerPreLoginEvent for " + uuid);

        loadPlayerData(uuid).thenAcceptAsync(hPlayer -> {
                    if (hPlayer == null) {
                        HPlayer newHPlayer = new HPlayer(uuid);
                        uuidToHPlayerMap.put(uuid, newHPlayer);
                        savePlayerData(newHPlayer)
                                .thenRun(() -> MessageUtil.log("Created new player entry for " + uuid))
                                .exceptionally(ex -> {
                                    MessageUtil.log("Failed to save new player entry for " + uuid + ": " + ex.getMessage());
                                    return null;
                                });
                        MessageUtil.log("Created shell HPlayer for new player " + uuid);
                    } else {
                        // Player loaded successfully, store in cache
                        uuidToHPlayerMap.put(uuid, hPlayer);
                        MessageUtil.log("Cached loaded HPlayer for " + uuid);
                    }
                }, runnable -> Bukkit.getScheduler().runTask(Hecate.getInstance(), runnable))
                .exceptionally(ex -> {
                    MessageUtil.log("Critical error loading player data for " + uuid + " during pre-login: " + ex.getMessage());
                    ex.printStackTrace();
                    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, Component.text("An error occurred while loading your data. Please try again later."));
                    return null;
                });
    }

    @EventHandler
    public void onLogin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        HPlayer hPlayer = uuidToHPlayerMap.get(uuid);

        if (hPlayer == null) {
            // This *shouldn't* happen if AsyncPlayerPreLoginEvent worked correctly
            MessageUtil.log("HPlayer not found in cache for " + uuid + " during PlayerJoinEvent! Creating new.");
            hPlayer = new HPlayer(uuid);
            uuidToHPlayerMap.put(uuid, hPlayer);
            savePlayerData(hPlayer);
        }

        hPlayer.setPlayer(player);
        MessageUtil.log("Associated Player object with HPlayer for " + uuid);

        // loadLastCharacterForPlayer(hPlayer);
    }

    @EventHandler
    public void onPlayerDataSave(PlayerDataSaveEvent event) { // Your custom event
        HPlayer hPlayer = uuidToHPlayerMap.get(event.getPlayer().getUniqueId());
        if (hPlayer != null) {
            MessageUtil.log("PlayerDataSaveEvent triggered for " + hPlayer.getPlayerId());
            savePlayerData(hPlayer);
            if (hPlayer.getSelectedCharacter() != null) {
                createOrUpdateCharacter(hPlayer.getSelectedCharacter());
            }
        }
    }

    @Override
    public void close() {
        MessageUtil.log("Performing final saves before shutdown...");
        CompletableFuture<?>[] saveFutures = getLoadedPlayers().stream()
                .map(hPlayer -> {
                    CompletableFuture<Boolean> saveCharacterFuture = null;
                    if (hPlayer.getSelectedCharacter() != null) {
                        saveCharacterFuture = createOrUpdateCharacter(hPlayer.getSelectedCharacter());
                    }
                    return saveCharacterFuture;
                })
                .toArray(CompletableFuture<?>[]::new);

        try {
            CompletableFuture.allOf(saveFutures).join(); // Wait for saves to complete
            MessageUtil.log("Final saves completed.");
        } catch(Exception e) {
            MessageUtil.log("Error during final saves: " + e.getMessage());
            e.printStackTrace();
        }

        super.close();
    }
}