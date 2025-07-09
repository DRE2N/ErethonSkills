package de.erethon.hecate.data.dao;

import de.erethon.bedrock.jdbi.v3.sqlobject.customizer.Bind;
import de.erethon.bedrock.jdbi.v3.sqlobject.statement.SqlQuery;
import de.erethon.bedrock.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;

public interface PlayerDao {

    @SqlQuery("SELECT last_character FROM Players WHERE player_id = :playerId")
    Optional<UUID> findLastCharacterId(@Bind("playerId") UUID playerId);

    @SqlQuery("SELECT EXISTS (SELECT 1 FROM Players WHERE player_id = :playerId)")
    boolean playerExists(@Bind("playerId") UUID playerId);

    @SqlUpdate("INSERT INTO Players (player_id, last_online, last_character) " +
            "VALUES (:playerId, :lastOnline, :lastCharacter) " +
            "ON CONFLICT (player_id) DO UPDATE SET " +
            "last_online = EXCLUDED.last_online, last_character = EXCLUDED.last_character")
    int upsertPlayer(@Bind("playerId") UUID playerId,
                     @Bind("lastOnline") Timestamp lastOnline,
                     @Bind("lastCharacter") UUID lastCharacter); // Can be null

    @SqlUpdate("INSERT INTO Players (player_id, last_online) VALUES (:playerId, NOW()) " +
            "ON CONFLICT (player_id) DO UPDATE SET last_online = EXCLUDED.last_online")
    int updateLastOnline(@Bind("playerId") UUID playerId);

    @SqlUpdate("UPDATE Players SET last_character = :lastCharacter WHERE player_id = :playerId")
    int updateLastCharacter(@Bind("playerId") UUID playerId, @Bind("lastCharacter") UUID lastCharacter);

    Timestamp getLastOnline(@Bind("playerId") UUID playerId);
}
