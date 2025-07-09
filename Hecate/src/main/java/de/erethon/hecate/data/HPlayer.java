package de.erethon.hecate.data;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.hecate.Hecate;
import de.erethon.hecate.events.PlayerSelectedCharacterEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.checkerframework.checker.units.qual.N;
import org.jetbrains.annotations.Nullable;

import java.sql.Timestamp;
import java.time.Duration;
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
    private Timestamp lastSeen;
    private @Nullable HCharacter selectedCharacter;
    private final List<HCharacter> characters;
    private @Nullable UUID lastCharacter;
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
        MessageUtil.log("Loaded " + characters.size() + " characters for player " + playerId);
    }

    public void setSelectedCharacter(HCharacter selectedCharacter, boolean dontSave) {
        if (selectedCharacter == null) {
            MessageUtil.log("Selected character is null for player " + player.getName());
            this.selectedCharacter = null; // Player is most likely in character selection screen and has no character selected yet
            return;
        }
        lock.lock();
        Title.Times times = Title.Times.times(Duration.ZERO, Duration.ofSeconds(999999), Duration.ZERO);
        Title title = Title.title(Component.empty(), Component.text("Loading character...", NamedTextColor.YELLOW), times);
        player.showTitle(title);
        try {
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
                                        setLastCharacter(selectedCharacter.getCharacterID());
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
                                        setLastCharacter(selectedCharacter.getCharacterID());
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
                                setLastCharacter(selectedCharacter.getCharacterID());
                            })
                            .exceptionally(ex -> {
                                ex.printStackTrace();
                                return null;
                            });
                    }
                }
            catch (Exception e) {
                MessageUtil.log("Failed to switch to character " + selectedCharacter.getCharacterID() + " for player " + player.getName());
                e.printStackTrace();
            }
        } finally {
            lock.unlock();
            BukkitRunnable mainTask = new BukkitRunnable() {
                @Override
                public void run() {
                    PlayerSelectedCharacterEvent event = new PlayerSelectedCharacterEvent(HPlayer.this, selectedCharacter, dontSave);
                    Hecate.getInstance().getServer().getPluginManager().callEvent(event);
                    player.removePotionEffect(PotionEffectType.BLINDNESS);
                    Title endTitle = Title.title(Component.empty(), Component.empty());
                    player.showTitle(endTitle);
                    player.setGameMode(GameMode.SURVIVAL); // We currently do not save this data, so let's just set it to survival
                }
            };
            mainTask.runTaskLater(Hecate.getInstance(), 10);
        }
    }

    public void setLastCharacter(@Nullable UUID lastCharacter) {
        this.lastCharacter = lastCharacter;
    }

    public @Nullable UUID getLastCharacter() {
        return lastCharacter;
    }

    public void setLastSeen(Timestamp lastSeen) {
        this.lastSeen = lastSeen;
    }

    public Timestamp getLastSeen() {
        return lastSeen;
    }
}