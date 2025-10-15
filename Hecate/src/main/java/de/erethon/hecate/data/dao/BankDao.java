package de.erethon.hecate.data.dao;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.Optional;
import java.util.UUID;

public interface BankDao {

    @SqlQuery("SELECT bank_contents FROM Banks WHERE player_id = :playerId")
    Optional<byte[]> findBankContents(@Bind("playerId") UUID playerId);

    @SqlQuery("SELECT unlocked_pages FROM Banks WHERE player_id = :playerId")
    Optional<Integer> findUnlockedPages(@Bind("playerId") UUID playerId);

    @SqlUpdate("INSERT INTO Banks (player_id, bank_contents, unlocked_pages) " +
            "VALUES (:playerId, :bankContents, :unlockedPages) " +
            "ON CONFLICT (player_id) DO UPDATE SET " +
            "bank_contents = EXCLUDED.bank_contents, unlocked_pages = EXCLUDED.unlocked_pages")
    int upsertBank(@Bind("playerId") UUID playerId,
                   @Bind("bankContents") byte[] bankContents,
                   @Bind("unlockedPages") int unlockedPages);

    @SqlQuery("SELECT EXISTS (SELECT 1 FROM Banks WHERE player_id = :playerId)")
    boolean bankExists(@Bind("playerId") UUID playerId);
}

