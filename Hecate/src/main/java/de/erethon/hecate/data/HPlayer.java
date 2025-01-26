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
    private @Nullable HCharacter selectedCharacter;
    private final List<HCharacter> characters;
    private int maximumCharacters = 3;

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

    /**
     * @return the selected HCharacter of the player
     * Will return null if the player has no selected character, e.g., is in the character selection screen
     */
    public @Nullable HCharacter getSelectedCharacter() {
        return selectedCharacter;
    }

    public List<HCharacter> getCharacters() {
        return characters;
    }

    public int getMaximumCharacters() {
        return maximumCharacters;
    }

    public void setCharacters(List<HCharacter> characters) {
        this.characters.clear();
        this.characters.addAll(characters);
        if (!characters.isEmpty()) {
            this.selectedCharacter = null;
        }
    }

    public void setSelectedCharacter(HCharacter selectedCharacter, boolean dontSave) {
        if (selectedCharacter == null) {
            this.selectedCharacter = null; // Player is most likely in character selection screen and has no character selected yet
            return;
        }
        lock.lock();
        try {
            if (this.selectedCharacter != null) {
                if (!dontSave) {
                    this.selectedCharacter.saveCharacterPlayerData(false)
                            .thenCompose(v -> {
                                this.selectedCharacter = selectedCharacter;
                                return selectedCharacter.loadCharacterPlayerData();
                            })
                            .thenRun(() -> {
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
                                MessageUtil.log("Switched to character " + selectedCharacter.getCharacterID() + " for player " + player.getName());
                            })
                            .exceptionally(ex -> {
                                ex.printStackTrace();
                                return null;
                            });
                }
            } else {
                this.selectedCharacter = selectedCharacter;
                selectedCharacter.loadCharacterPlayerData()
                        .thenRun(() -> {
                            if (!dontSave) {
                                selectedCharacter.saveCharacterPlayerData(false);
                            }
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
}