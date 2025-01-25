package de.erethon.hecate.data;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.hecate.Hecate;
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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class DatabaseManager implements Listener {

    private final HikariDataSource dataSource;
    private final HashMap<Player, HPlayer> playerToHPlayerMap = new HashMap<>();
    private final HashMap<UUID, HPlayer> uuidToHPlayerMap = new HashMap<>();

    public DatabaseManager() {
        File configFile = new File("environment.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        String dbUrl = config.getString("dbUrl");
        String dbUser = config.getString("dbUser");
        String dbPassword = config.getString("dbPassword");

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(dbUrl);
        hikariConfig.setUsername(dbUser);
        hikariConfig.setPassword(dbPassword);
        this.dataSource = new HikariDataSource(hikariConfig);
        Bukkit.getPluginManager().registerEvents(this, Hecate.getInstance());
    }

    public CompletableFuture<Void> createTables() {
        String createPlayersTable = "CREATE TABLE IF NOT EXISTS Players (" +
                "player_id CHAR(36) PRIMARY KEY," +
                "last_online TIMESTAMP)";

        String createCharactersTable = "CREATE TABLE IF NOT EXISTS Characters (" +
                "character_id CHAR(36) PRIMARY KEY," +
                "player_id CHAR(36) REFERENCES Players(player_id)," +
                "level INT," +
                "class_id VARCHAR(255)," +
                "playerdata BLOB," +
                "created_at TIMESTAMP," +
                "locked_by TEXT," +
                "skills TEXT," +
                "UNIQUE (character_id))";

        String createCompletedQuestsTable = "CREATE TABLE IF NOT EXISTS Completed_Quests (" +
                "character_id CHAR(36) REFERENCES Characters(character_id)," +
                "quest_id VARCHAR(255)," +
                "completed_at TIMESTAMP," +
                "PRIMARY KEY (character_id, quest_id))";

        return executeUpdate(createPlayersTable)
                .thenCompose(v -> executeUpdate(createCharactersTable))
                .thenCompose(v -> executeUpdate(createCompletedQuestsTable));
    }

    private CompletableFuture<Void> executeUpdate(String query) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 Statement stmt = connection.createStatement()) {
                stmt.executeUpdate(query);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public CompletableFuture<Boolean> createOrUpdateCharacter(HCharacter character) {
        String query = "INSERT INTO Characters (character_id, player_id, level, class_id, playerdata, created_at, skills) " +
                "VALUES (?, ?, ?, ?, ?, NOW(), ?) ON DUPLICATE KEY UPDATE " +
                "level = VALUES(level), class_id = VALUES(class_id), playerdata = VALUES(playerdata), skills = VALUES(skills)";
        byte[] playerData = character.serializePlayerDataToBlob();
        return executeUpdateWithParams(query, character.getCharacterID().toString(), character.getHPlayer().getPlayerId().toString(),
                character.getLevel(), character.getClassId(), playerData, String.join(",", character.getSkills()))
                .thenApply(rowsAffected -> {
                    if (rowsAffected > 0) {
                        return true;
                    } else {
                        return false;
                    }
                });
    }

    private CompletableFuture<HPlayer> loadPlayerData(UUID uuid) {
        String query = "SELECT player_id FROM Players WHERE player_id = ?";
        return executeQuery(query, uuid.toString())
                .thenCompose(results -> {
                    if (results == null || results.isEmpty()) {
                        return CompletableFuture.completedFuture(null);
                    }
                    HPlayer hPlayer = new HPlayer(uuid);
                    MessageUtil.log("Loaded player data for " + uuid);
                    uuidToHPlayerMap.put(hPlayer.getPlayerId(), hPlayer);
                    return getCharactersForPlayer(uuid).thenApply(characters -> {
                        hPlayer.setCharacters(characters);
                        return hPlayer;
                    });
                });
    }

    public CompletableFuture<Boolean> saveCharacterPlayerData(UUID characterId, byte[] playerData) {
        String query = "UPDATE Characters SET playerdata = ? WHERE character_id = ?";
        return executeUpdateWithParams(query, playerData, characterId.toString())
                .thenApply(v -> v == 1);
    }

    public CompletableFuture<byte[]> loadCharacterPlayerData(UUID characterId) {
        String query = "SELECT playerdata FROM Characters WHERE character_id = ?";
        return executeQuery(query, characterId.toString())
                .thenApply(results -> {
                    if (results == null || results.isEmpty()) {
                        return null;
                    }
                    return (byte[]) results.getFirst().get("playerdata");
                });
    }

    public HPlayer getHPlayer(Player player) {
        return playerToHPlayerMap.get(player);
    }

    public HPlayer getHPlayer(UUID uuid) {
        return uuidToHPlayerMap.get(uuid);
    }

    public Collection<HPlayer> getPlayers() {
        return playerToHPlayerMap.values();
    }

    public CompletableFuture<List<HCharacter>> getCharactersForPlayer(UUID playerId) {
        HPlayer hPlayer = uuidToHPlayerMap.get(playerId);
        if (hPlayer == null) {
            MessageUtil.log("Player " + playerId + " not found in cache. Skipping character load.");
            return CompletableFuture.completedFuture(new ArrayList<>());
        }

        String query = "SELECT character_id, level, class_id, playerdata, created_at, skills FROM Characters WHERE player_id = ?";
        return executeQuery(query, playerId.toString())
                .thenApply(results -> {
                    List<HCharacter> characters = new ArrayList<>();
                    for (Map<String, Object> row : results) {
                        UUID characterId = UUID.fromString((String) row.get("character_id"));
                        int level = (int) row.get("level");
                        String classId = (String) row.get("class_id");
                        Timestamp createdAt = (Timestamp) row.get("created_at");
                        String skillsString = (String) row.get("skills");
                        List<String> skills = List.of(skillsString.split(","));
                        characters.add(new HCharacter(characterId, hPlayer, level, classId, createdAt, skills));
                    }
                    return characters;
                });
    }

    public CompletableFuture<Boolean> selectAndUseCharacter(UUID characterId, String lockOwner) {
        return lockPlayerData(characterId, lockOwner)
                .thenCompose(lockSuccess -> {
                    if (lockSuccess) {
                        return getCharacterData(characterId)
                                .thenApply(characterData -> {
                                    if (characterData != null) {
                                        System.out.println("Character Data: " + characterData);
                                        return true;
                                    }
                                    return false;
                                });
                    }
                    return CompletableFuture.completedFuture(false);
                });
    }

    public CompletableFuture<Void> savePlayerData(HPlayer hPlayer) {
        String query = "INSERT INTO Players (player_id, last_online) VALUES (?, NOW()) " +
                "ON DUPLICATE KEY UPDATE last_online = NOW()";
        return executeUpdateWithParams(query, hPlayer.getPlayerId().toString())
                .exceptionally(ex -> {
                    if (ex.getCause() instanceof SQLIntegrityConstraintViolationException) {
                        return 0;
                    }
                    throw new CompletionException(ex);
                }).thenApply(result -> null);
    }

    public CompletableFuture<Boolean> lockPlayerData(UUID characterId, String lockOwner) {
        String lockQuery = "UPDATE Characters SET locked_by = ? WHERE character_id = ? AND locked_by IS NULL";
        return executeUpdateWithParams(lockQuery, lockOwner, characterId.toString())
                .thenApply(v -> v == 1);
    }

    public CompletableFuture<Void> unlockPlayerData(UUID characterId) {
        String unlockQuery = "UPDATE Characters SET locked_by = NULL WHERE character_id = ?";
        return executeUpdateWithParams(unlockQuery, characterId.toString()).thenApply(result -> null);
    }

    public CompletableFuture<HCharacter> getCharacterData(UUID characterId) {
        String query = "SELECT character_id, level, class_id, playerdata, created_at, locked_by, skills FROM Characters WHERE character_id = ?";
        return executeQuery(query, characterId.toString())
                .thenApply(results -> {
                    if (results == null || results.isEmpty()) {
                        return null;
                    }
                    Map<String, Object> row = results.get(0);
                    UUID id = UUID.fromString((String) row.get("character_id"));
                    int level = (int) row.get("level");
                    String classId = (String) row.get("class_id");
                    Timestamp createdAt = (Timestamp) row.get("created_at");
                    String lockedBy = (String) row.get("locked_by");
                    String skillsString = (String) row.get("skills");
                    List<String> skills = List.of(skillsString.split(","));
                    HPlayer hPlayer = playerToHPlayerMap.values().stream()
                            .filter(p -> p.getCharacters().stream().anyMatch(c -> c.getCharacterID().equals(id)))
                            .findFirst()
                            .orElse(null);
                    return new HCharacter(id, hPlayer, level, classId, createdAt, lockedBy, skills);
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
            hPlayer.saveToDatabase(this);
            hPlayer.getSelectedCharacter().saveCharacterPlayerData();
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

}