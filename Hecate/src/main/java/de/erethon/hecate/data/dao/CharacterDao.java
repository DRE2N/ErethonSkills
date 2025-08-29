package de.erethon.hecate.data.dao;

import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RegisterBeanMapper(CharacterDao.FlatData.class)
public interface CharacterDao {

    class FlatData {
        public UUID characterId;
        public UUID playerId;
        public int level;
        public String classId;
        public Timestamp createdAt;
        public String lockedBy;
        public String skills;
        public String traitline;
        public Integer[] selectedTraits;
        public byte[] playerdata;

        public UUID getCharacterId() { return characterId; }
        public UUID getPlayerId() { return playerId; }
        public int getLevel() { return level; }
        public String getClassId() { return classId; }
        public Timestamp getCreatedAt() { return createdAt; }
        public String getLockedBy() { return lockedBy; }
        public String getSkills() { return skills; }
        public String getTraitline() { return traitline; }
        public Integer[] getSelectedTraits() { return selectedTraits; }
        public byte[] getPlayerdata() { return playerdata; }

        public void setCharacterId(UUID characterId) { this.characterId = characterId; }
        public void setPlayerId(UUID playerId) { this.playerId = playerId; }
        public void setLevel(int level) { this.level = level; }
        public void setClassId(String classId) { this.classId = classId; }
        public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
        public void setLockedBy(String lockedBy) { this.lockedBy = lockedBy; }
        public void setSkills(String skills) { this.skills = skills; }
        public void setTraitline(String traitline) { this.traitline = traitline; }
        public void setSelectedTraits(Integer[] selectedTraits) { this.selectedTraits = selectedTraits; }
        public void setPlayerdata(byte[] playerdata) { this.playerdata = playerdata; }
    }


    @SqlUpdate("INSERT INTO Characters (character_id, player_id, level, class_id, playerdata, created_at, skills, traitline, selected_traits, locked_by) " +
            "VALUES (:characterId, :playerId, :level, :classId, :playerdata, NOW(), :skills, :traitline, :selectedTraits, NULL) " +
            "ON CONFLICT (character_id) DO UPDATE SET " +
            "level = EXCLUDED.level, " +
            "class_id = EXCLUDED.class_id, " +
            "playerdata = EXCLUDED.playerdata, " +
            "skills = EXCLUDED.skills, " +
            "traitline = EXCLUDED.traitline, " +
            "selected_traits = EXCLUDED.selected_traits")
    int upsertCharacter(@Bind("characterId") UUID characterId,
                        @Bind("playerId") UUID playerId,
                        @Bind("level") int level,
                        @Bind("classId") String classId,
                        @Bind("playerdata") byte[] playerdata,
                        @Bind("skills") String skills,
                        @Bind("traitline") String traitline,
                        @Bind("selectedTraits") Integer[] selectedTraits);


    @SqlQuery("SELECT character_id, player_id, level, class_id, created_at, locked_by, skills, traitline, selected_traits " +
            "FROM Characters WHERE player_id = :playerId")
    List<FlatData> findCharactersByPlayerId(@Bind("playerId") UUID playerId);

    @SqlQuery("SELECT character_id, player_id, level, class_id, created_at, locked_by, skills, traitline, selected_traits " +
            "FROM Characters WHERE character_id = :characterId")
    Optional<FlatData> findCharacterById(@Bind("characterId") UUID characterId);

    @SqlQuery("SELECT playerdata FROM Characters WHERE character_id = :characterId")
    Optional<byte[]> findCharacterPlayerData(@Bind("characterId") UUID characterId);

    @SqlUpdate("UPDATE Characters SET playerdata = :playerdata WHERE character_id = :characterId")
    int updateCharacterPlayerData(@Bind("characterId") UUID characterId, @Bind("playerdata") byte[] playerdata);

    // Lock: Update locked_by only if it's currently NULL
    @SqlUpdate("UPDATE Characters SET locked_by = :lockOwner WHERE character_id = :characterId AND locked_by IS NULL")
    int lockCharacter(@Bind("characterId") UUID characterId, @Bind("lockOwner") String lockOwner);

    @SqlUpdate("UPDATE Characters SET locked_by = NULL WHERE character_id = :characterId")
    int unlockCharacter(@Bind("characterId") UUID characterId);

    @SqlQuery("SELECT locked_by FROM Characters WHERE character_id = :characterId")
    Optional<String> getLockOwner(@Bind("characterId") UUID characterId);

    @SqlUpdate("DELETE FROM Characters WHERE character_id = :characterId")
    int deleteCharacter(@Bind("characterId") UUID characterId);

}
