package de.erethon.hecate.data;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.hecate.Hecate;
import de.erethon.hecate.events.PlayerSelectedCharacterEvent;
import de.erethon.hecate.progression.LevelUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.world.entity.player.Abilities;
import org.bukkit.GameMode;
import org.bukkit.craftbukkit.entity.CraftPlayer;
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
        Hecate.log("Loaded " + characters.size() + " characters for player " + playerId);
    }

    public void setSelectedCharacter(HCharacter selectedCharacter, boolean dontSave) {
        if (selectedCharacter == null) {
            Hecate.log("Selected character is null for player " + player.getName());
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
                        // Save the current gamemode before switching
                        this.selectedCharacter.setGamemode(player.getGameMode().toString());
                        if (!dontSave) {
                            this.selectedCharacter.saveCharacterPlayerData(false)
                                    .thenCompose(v -> {
                                        this.selectedCharacter = selectedCharacter;
                                        return selectedCharacter.loadCharacterPlayerData();
                                    })
                                    .thenRun(() -> {
                                        Hecate.log("Switched to character " + selectedCharacter.getCharacterID() + " for player " + player.getName());
                                        setLastCharacter(selectedCharacter.getCharacterID());
                                        clearBlindnessAndTitle();
                                    })
                                    .exceptionally(ex -> {
                                        ex.printStackTrace();
                                        clearBlindnessAndTitle();
                                        return null;
                                    });
                        } else {
                            this.selectedCharacter = selectedCharacter;
                            selectedCharacter.loadCharacterPlayerData()
                                    .thenRun(() -> {
                                        Hecate.log("Switched to character " + selectedCharacter.getCharacterID() + " for player " + player.getName());
                                        setLastCharacter(selectedCharacter.getCharacterID());
                                        clearBlindnessAndTitle();
                                    })
                                    .exceptionally(ex -> {
                                        ex.printStackTrace();
                                        clearBlindnessAndTitle();
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
                                Hecate.log("Switched to character " + selectedCharacter.getCharacterID() + " for player " + player.getName());
                                setLastCharacter(selectedCharacter.getCharacterID());
                                clearBlindnessAndTitle();
                            })
                            .exceptionally(ex -> {
                                ex.printStackTrace();
                                clearBlindnessAndTitle();
                                return null;
                            });
                    }
                }
            catch (Exception e) {
                Hecate.log("Failed to switch to character " + selectedCharacter.getCharacterID() + " for player " + player.getName());
                e.printStackTrace();
                clearBlindnessAndTitle();
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
                    // Restore the saved gamemode if it exists
                    String savedGamemode = selectedCharacter.getGamemode();
                    if (savedGamemode != null && !savedGamemode.isEmpty()) {
                        try {
                            GameMode gameMode = GameMode.valueOf(savedGamemode);
                            player.setGameMode(gameMode);
                        } catch (IllegalArgumentException e) {
                            player.setGameMode(GameMode.SURVIVAL);
                            MessageUtil.log("Invalid gamemode '" + savedGamemode + "' for character " + selectedCharacter.getCharacterID() + " of player " + player.getName() + ". Defaulting to SURVIVAL.");
                        }
                    } else {
                        player.setGameMode(GameMode.SURVIVAL);
                        MessageUtil.log("No gamemode saved for character " + selectedCharacter.getCharacterID() + " of player " + player.getName());
                    }
                    // This is incredibly buggy, but I am annoyed by the complaints
                    CraftPlayer craftPlayer = (CraftPlayer) player;
                    craftPlayer.getHandle().onUpdateAbilities();
                    float modeId = switch (player.getGameMode()) {
                        case SURVIVAL -> 0;
                        case CREATIVE -> 1;
                        case ADVENTURE -> 2;
                        case SPECTATOR -> 3;
                    };
                    craftPlayer.getHandle().connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.CHANGE_GAME_MODE, modeId));

                    LevelUtil.displayCharLevel(player.getPlayer());
                }
            };
            mainTask.runTaskLater(Hecate.getInstance(), 10);
        }
    }

    private void clearBlindnessAndTitle() {
        if (player != null && player.isOnline()) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    Title clearTitle = Title.title(Component.empty(), Component.empty());
                    player.showTitle(clearTitle);
                }
            }.runTask(Hecate.getInstance());
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