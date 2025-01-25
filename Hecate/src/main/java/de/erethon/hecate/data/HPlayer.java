package de.erethon.hecate.data;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.hecate.Hecate;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class HPlayer {

    private final DatabaseManager databaseManager = Hecate.getInstance().getDatabaseManager();
    private final ReentrantLock lock = new ReentrantLock();

    private final UUID playerId;
    private Player player;
    private HCharacter selectedCharacter;
    private final List<HCharacter> characters;

    public HPlayer(UUID uuid) {
        this.playerId = uuid;
        this.characters = new ArrayList<>();
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public @Nullable Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public HCharacter getSelectedCharacter() {
        return selectedCharacter;
    }

    public List<HCharacter> getCharacters() {
        return characters;
    }

    public void setCharacters(List<HCharacter> characters) {
        this.characters.clear();
        this.characters.addAll(characters);
        if (!characters.isEmpty()) {
            this.selectedCharacter = characters.getFirst();
        }
    }

    public void setSelectedCharacter(HCharacter selectedCharacter) {
        lock.lock();
        try {
            if (this.selectedCharacter != null) {
                this.selectedCharacter.saveCharacterPlayerData()
                        .thenCompose(v -> {
                            this.selectedCharacter = selectedCharacter;
                            return selectedCharacter.loadCharacterPlayerData();
                        })
                        .thenRun(() -> {
                            saveToDatabase(databaseManager);
                            MessageUtil.log("Switched to character " + selectedCharacter.getCharacterID() + " for player " + player.getName());
                        })
                        .exceptionally(ex -> {
                            ex.printStackTrace();
                            return null;
                        });
            } else {
                this.selectedCharacter = selectedCharacter;
                selectedCharacter.loadCharacterPlayerData()
                        .thenRun(() -> {
                            saveToDatabase(databaseManager);
                            MessageUtil.log("Switched to character " + selectedCharacter.getCharacterID() + " for player " + player.getName());
                        })
                        .exceptionally(ex -> {
                            ex.printStackTrace();
                            return null;
                        });
            }
        } finally {
            lock.unlock();
        }
    }

    public void saveToDatabase(DatabaseManager dbManager) {
        List<CompletableFuture<Void>> futures = characters.stream()
                .map(character -> character.saveToDatabase(dbManager))
                .toList();
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }
}