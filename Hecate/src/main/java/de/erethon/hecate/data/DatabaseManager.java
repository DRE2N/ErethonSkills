package de.erethon.hecate.data;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.hecate.Hecate;
import de.erethon.papyrus.events.PlayerDataSaveEvent;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class DatabaseManager implements Listener {

    private HikariDataSource dataSource;
    private final HashMap<Player, HPlayer> playerToHPlayerMap = new HashMap<>();
    private final HashMap<UUID, HPlayer> uuidToHPlayerMap = new HashMap<>();

    public DatabaseManager() {
        File configFile = new File("environment.yml");
        if (!configFile.exists()) {
            MessageUtil.log("--------------------------------------------------");
            MessageUtil.log(" ");
            MessageUtil.log(" ");
            MessageUtil.log("Please create a file named 'environment.yml' in the root directory of the server.");
            MessageUtil.log("This file should contain the database connection information.");
            MessageUtil.log("Shutting server down...");
            MessageUtil.log(" ");
            MessageUtil.log(" ");
            MessageUtil.log("--------------------------------------------------");
            Bukkit.getServer().shutdown();
            return;
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        String dbUrl = config.getString("dbUrl");
        String dbUser = config.getString("dbUser");
        String dbPassword = config.getString("dbPassword");

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(dbUrl);
        hikariConfig.setDataSourceClassName("org.postgresql.ds.PGSimpleDataSource");
        hikariConfig.setUsername(dbUser);
        hikariConfig.setPassword(dbPassword);
        this.dataSource = new HikariDataSource(hikariConfig);
        Bukkit.getPluginManager().registerEvents(this, Hecate.getInstance());

        createTables().join();
    }

    public CompletableFuture<Void> createTables() {
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
                "IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_last_character') THEN " +
                "ALTER TABLE Players ADD CONSTRAINT fk_last_character FOREIGN KEY (last_character) REFERENCES Characters(character_id); " +
                "END IF; " +
                "END $$;";

        String addForeignKeyToCharacters = "DO $$ BEGIN " +
                "IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_player_id') THEN " +
                "ALTER TABLE Characters ADD CONSTRAINT fk_player_id FOREIGN KEY (player_id) REFERENCES Players(player_id); " +
                "END IF; " +
                "END $$;";

        String addForeignKeyToCompletedQuests = "DO $$ BEGIN " +
                "IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_character_id') THEN " +
                "ALTER TABLE Completed_Quests ADD CONSTRAINT fk_character_id FOREIGN KEY (character_id) REFERENCES Characters(character_id); " +
                "END IF; " +
                "END $$;";

        // Upgrade tables if we add new columns. Never remove those, we might have old data.
        List<String> alterTableStatements = Arrays.asList(
                "ALTER TABLE Characters ADD COLUMN IF NOT EXISTS traitline VARCHAR(255)",
                "ALTER TABLE Characters ADD COLUMN IF NOT EXISTS selected_traits INTEGER[]"
        );

        return executeUpdate(createCharactersTable)
                .thenCompose(v -> executeUpdate(createPlayersTable))
                .thenCompose(v -> executeUpdate(createCompletedQuestsTable))
                .thenCompose(v -> executeUpdate(addForeignKeyToPlayers))
                .thenCompose(v -> executeUpdate(addForeignKeyToCharacters))
                .thenCompose(v -> executeUpdate(addForeignKeyToCompletedQuests))
                .thenCompose(v -> {
                    CompletableFuture<Void> future = CompletableFuture.completedFuture(null);
                    for (String statement : alterTableStatements) {
                        future = future.thenCompose(v2 -> executeUpdate(statement));
                    }
                    return future;
                })
                .thenAccept(v -> MessageUtil.log("Database initialization complete"))
                .exceptionally(ex -> {
                    MessageUtil.log("Error creating tables or adding foreign keys: " + ex.getMessage());
                    ex.printStackTrace();
                    return null;
                });
    }

    private CompletableFuture<Void> executeUpdate(String query) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 Statement stmt = connection.createStatement()) {
                MessageUtil.log("Executing update: " + query);
                stmt.executeUpdate(query);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public CompletableFuture<Boolean> createOrUpdateCharacter(HCharacter character) {
        String query = "INSERT INTO Characters (character_id, player_id, level, class_id, playerdata, created_at, skills, traitline, selected_traits) " +
                "VALUES (?, ?, ?, ?, ?, NOW(), ?, ?, ?) ON CONFLICT (character_id) DO UPDATE SET " +
                "level = EXCLUDED.level, class_id = EXCLUDED.class_id, playerdata = EXCLUDED.playerdata, skills = EXCLUDED.skills, " +
                "traitline = EXCLUDED.traitline, selected_traits = EXCLUDED.selected_traits";
        byte[] playerData = character.serializePlayerDataToBlob(false);
        String traitline = character.getTraitline().getId();
        return executeUpdateWithParams(query, character.getCharacterID(), character.getHPlayer().getPlayerId(),
                character.getLevel(), character.getClassId(), playerData, String.join(",", character.getSkills()),
                traitline, character.getSelectedTraits())
                .thenApply(rowsAffected -> rowsAffected > 0);
    }

    private CompletableFuture<HPlayer> loadPlayerData(UUID uuid) {
        String query = "SELECT player_id FROM Players WHERE player_id = ?";
        return executeQuery(query, uuid)
                .thenCompose(results -> {
                    if (results == null || results.isEmpty()) {
                        return CompletableFuture.completedFuture(null);
                    }
                    HPlayer hPlayer = new HPlayer(uuid);
                    MessageUtil.log("Loaded player data for " + uuid);
                    uuidToHPlayerMap.put(hPlayer.getPlayerId(), hPlayer);
                    return getCharactersForPlayer(uuid).thenApply(characters -> {
                        MessageUtil.log("Loaded " + characters.size() + " characters for player " + uuid);
                        hPlayer.setCharacters(characters);
                        return hPlayer;
                    });
                });
    }

    public CompletableFuture<Boolean> saveCharacterPlayerData(UUID characterId, byte[] playerData) {
        String query = "UPDATE Characters SET playerdata = ? WHERE character_id = ?";
        return executeUpdateWithParams(query, playerData, characterId)
                .thenApply(v -> v == 1);
    }

    public CompletableFuture<byte[]> loadCharacterPlayerData(UUID characterId) {
        String query = "SELECT playerdata FROM Characters WHERE character_id = ?";
        return executeQuery(query, characterId)
                .thenApply(results -> {
                    if (results == null || results.isEmpty()) {
                        return null;
                    }
                    return (byte[]) results.get(0).get("playerdata");
                });
    }

    public HPlayer getHPlayer(Player player) {
        return playerToHPlayerMap.get(player);
    }

    public HPlayer getHPlayer(UUID uuid) {
        return uuidToHPlayerMap.get(uuid);
    }

    public HCharacter getCurrentCharacter(Player player) {
        HPlayer hPlayer = playerToHPlayerMap.get(player);
        if (hPlayer == null) {
            return null;
        }
        return hPlayer.getSelectedCharacter();
    }

    public Collection<HPlayer> getPlayers() {
        return playerToHPlayerMap.values();
    }

    public CompletableFuture<List<HCharacter>> getCharactersForPlayer(UUID playerId) {
        MessageUtil.log("Querying characters for player " + playerId);
        HPlayer hPlayer = uuidToHPlayerMap.get(playerId);
        if (hPlayer == null) {
            MessageUtil.log("Player " + playerId + " not found in cache. Skipping character load.");
            return CompletableFuture.completedFuture(new ArrayList<>());
        }

        String query = "SELECT character_id, level, class_id, playerdata, created_at, skills FROM Characters WHERE player_id = ?";
        return executeQuery(query, playerId)
                .thenApply(results -> {
                    if (results == null || results.isEmpty()) {
                        MessageUtil.log("No characters found for player " + playerId);
                        return new ArrayList<>();
                    }
                    List<HCharacter> characters = new ArrayList<>();
                    for (Map<String, Object> row : results) {
                        try {
                            UUID characterId = (UUID) row.get("character_id");
                            int level = (int) row.get("level");
                            String classId = (String) row.get("class_id");
                            Timestamp createdAt = (Timestamp) row.get("created_at");
                            String skillsString = (String) row.get("skills");
                            List<String> skills = List.of(skillsString.split(","));
                            characters.add(new HCharacter(characterId, hPlayer, level, classId, createdAt, skills));
                            MessageUtil.log("Loaded character " + characterId + " for player " + playerId);
                        } catch (Exception e) {
                            MessageUtil.log("Error processing row: " + row + " for player " + playerId + ": " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                    MessageUtil.log("Loaded " + characters.size() + " characters for player " + playerId);
                    return characters;
                });
    }

    public CompletableFuture<Boolean> selectAndUseCharacter(UUID characterId, String lockOwner) {
        return lockPlayerData(characterId, lockOwner)
                .thenCompose(lockSuccess -> {
                    if (lockSuccess) {
                        return getCharacterData(characterId)
                                .thenApply(characterData -> characterData != null);
                    }
                    return CompletableFuture.completedFuture(false);
                });
    }

    public CompletableFuture<Void> savePlayerData(HPlayer hPlayer) {
        String query = "INSERT INTO Players (player_id, last_online) VALUES (?, NOW()) " +
                "ON CONFLICT (player_id) DO UPDATE SET last_online = EXCLUDED.last_online";
        return executeUpdateWithParams(query, hPlayer.getPlayerId())
                .exceptionally(ex -> {
                    if (ex.getCause() instanceof SQLIntegrityConstraintViolationException) {
                        return 0;
                    }
                    throw new CompletionException(ex);
                }).thenApply(result -> null);
    }

    public CompletableFuture<Boolean> lockPlayerData(UUID characterId, String lockOwner) {
        String lockQuery = "UPDATE Characters SET locked_by = ? WHERE character_id = ? AND locked_by IS NULL";
        return executeUpdateWithParams(lockQuery, lockOwner, characterId)
                .thenApply(v -> v == 1);
    }

    public CompletableFuture<Void> unlockPlayerData(UUID characterId) {
        String unlockQuery = "UPDATE Characters SET locked_by = NULL WHERE character_id = ?";
        return executeUpdateWithParams(unlockQuery, characterId).thenApply(result -> null);
    }

    public CompletableFuture<HCharacter> getCharacterData(UUID characterId) {
        String query = "SELECT character_id, level, class_id, playerdata, created_at, locked_by, skills, traitline, selected_traits FROM Characters WHERE character_id = ?";
        return executeQuery(query, characterId)
                .thenApply(results -> {
                    if (results == null || results.isEmpty()) {
                        return null;
                    }
                    Map<String, Object> row = results.get(0);
                    UUID id = (UUID) row.get("character_id");
                    int level = (int) row.get("level");
                    String classId = (String) row.get("class_id");
                    Timestamp createdAt = (Timestamp) row.get("created_at");
                    String lockedBy = (String) row.get("locked_by");
                    String skillsString = (String) row.get("skills");
                    List<String> skills = List.of(skillsString.split(","));
                    String traitline = (String) row.get("traitline");
                    Integer[] selectedTraitsArray = (Integer[]) row.get("selected_traits");
                    HPlayer hPlayer = playerToHPlayerMap.values().stream()
                            .filter(p -> p.getCharacters().stream().anyMatch(c -> c.getCharacterID().equals(id)))
                            .findFirst()
                            .orElse(null);
                    return new HCharacter(id, hPlayer, level, classId, createdAt, lockedBy, skills, traitline, selectedTraitsArray);
                });
    }

    private CompletableFuture<Integer> executeUpdateWithParams(String query, Object... params) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement stmt = connection.prepareStatement(query)) {
                for (int i = 0; i < params.length; i++) {
                    stmt.setObject(i + 1, params[i]);
                }
                return stmt.executeUpdate();
            } catch (SQLException e) {
                MessageUtil.log("SQL Exception: " + e.getMessage() + " for query: " + query);
                e.printStackTrace();
                return 0;
            }
        });
    }

    private CompletableFuture<List<Map<String, Object>>> executeQuery(String query, Object... params) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement stmt = connection.prepareStatement(query)) {
                for (int i = 0; i < params.length; i++) {
                    stmt.setObject(i + 1, params[i]);
                }
                MessageUtil.log("Executing query: " + query + " with params: " + Arrays.toString(params));
                try (ResultSet resultSet = stmt.executeQuery()) {
                    List<Map<String, Object>> results = new ArrayList<>();
                    ResultSetMetaData metaData = resultSet.getMetaData();
                    int columnCount = metaData.getColumnCount();
                    while (resultSet.next()) {
                        Map<String, Object> row = new HashMap<>();
                        for (int i = 1; i <= columnCount; i++) {
                            row.put(metaData.getColumnName(i), resultSet.getObject(i));
                        }
                        results.add(row);
                    }
                    MessageUtil.log("Query executed successfully, number of rows: " + results.size());
                    return results;
                }
            } catch (SQLException e) {
                MessageUtil.log("SQL Exception: " + e.getMessage() + " for query: " + query);
                e.printStackTrace();
                return null;
            }
        });
    }

    public void close() {
        dataSource.close();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        HPlayer hPlayer = playerToHPlayerMap.get(player);
        if (hPlayer != null) {
            savePlayerData(hPlayer);
            hPlayer.getSelectedCharacter().saveCharacterPlayerData(false);
            playerToHPlayerMap.remove(player);
            uuidToHPlayerMap.remove(player.getUniqueId());
        }
    }

    @EventHandler
    public void onLogin(AsyncPlayerPreLoginEvent event) {
        UUID uuid = event.getUniqueId();
        loadPlayerData(uuid).thenAccept(hPlayer -> {
            if (hPlayer == null) {
                HPlayer newHPlayer = new HPlayer(uuid);
                uuidToHPlayerMap.put(uuid, newHPlayer);
                savePlayerData(newHPlayer);
            } else {
                uuidToHPlayerMap.put(uuid, hPlayer);
            }
        });
    }

    @EventHandler
    public void onLogin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        HPlayer hPlayer = uuidToHPlayerMap.get(player.getUniqueId());
        if (hPlayer == null) {
            hPlayer = new HPlayer(player.getUniqueId());
        }
        playerToHPlayerMap.put(player, hPlayer);
        hPlayer.setPlayer(player);
    }

    @EventHandler
    public void onPlayerDataSave(PlayerDataSaveEvent event) {
        HPlayer hPlayer = uuidToHPlayerMap.get(event.getPlayer().getUniqueId());
        savePlayerData(hPlayer);
    }

}