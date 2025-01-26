package de.erethon.hecate.charselection;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.hecate.Hecate;
import de.erethon.hecate.data.DatabaseManager;
import de.erethon.hecate.data.HCharacter;
import de.erethon.hecate.data.HPlayer;
import de.erethon.hecate.events.PlayerSelectedCharacterEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class CharacterSelection implements Listener {

    private final Hecate plugin = Hecate.getInstance();
    private final DatabaseManager databaseManager = plugin.getDatabaseManager();

    private final Player player;
    private final HPlayer hPlayer;
    private final CharacterLobby lobby;
    private List<CharacterDisplay> displayedCharacters = new ArrayList<>();
    private final Set<TextDisplay> emptySlotDisplays = new HashSet<>();
    private final Interaction[] interactions = new Interaction[9];
    private boolean confirmed = false;
    private boolean playerIsDone = false;

    public CharacterSelection(Player player, CharacterLobby lobby) {
        this.player = player;
        this.lobby = lobby;
        this.hPlayer = plugin.getDatabaseManager().getHPlayer(player);
        if (hPlayer.getSelectedCharacter() != null && hPlayer.getSelectedCharacter().isInCastMode()) {
            MessageUtil.sendMessage(player, "<red>You can't select a character while in combat mode.");
            return;
        }
        Bukkit.getPluginManager().registerEvents(this, plugin);
        List<HCharacter> characters = hPlayer.getCharacters();
        for (HCharacter character : characters) {
            CharacterDisplay display = new CharacterDisplay(character, this);
            displayedCharacters.add(display);
        }
        if (hPlayer.getSelectedCharacter() == null) {
            setup();
            return;
        }
        hPlayer.getSelectedCharacter().saveToDatabase(databaseManager).thenAccept(v -> { // make sure the current character is saved first
            setup();
            hPlayer.setSelectedCharacter(null, false);
        });

    }

    private void setup() {
        player.teleportAsync(lobby.getOrigin()).thenAccept(a -> {
            List<Location> locations = lobby.getPedestalLocations();
            for (int i = 0; i < locations.size(); i++) {
                if (i >= hPlayer.getMaximumCharacters()) {
                    spawnLockedSlotDisplay(locations.get(i));
                    Interaction interaction = spawnInteractionEntity(locations.get(i));
                    interactions[i] = interaction;
                } else if (i < displayedCharacters.size()) {
                    displayedCharacters.get(i).display(player, locations.get(i));
                    spawnCharacterInfoDisplay(displayedCharacters.get(i).getCharacter(), locations.get(i));
                } else {
                    spawnEmptySlotDisplay(locations.get(i));
                    Interaction interaction = spawnInteractionEntity(locations.get(i));
                    interactions[i] = interaction;
                }
            }
        });
    }

    private void leaveCharacterSelection(boolean newCharacter) {
        for (CharacterDisplay characterDisplay : displayedCharacters) {
            characterDisplay.remove(player);
        }
        for (TextDisplay emptySlotDisplay : emptySlotDisplays) {
            emptySlotDisplay.remove();
        }
        for (Interaction interaction : interactions) {
            if (interaction == null) continue;
            interaction.remove();
        }
        player.setInvisible(false);
        HandlerList.unregisterAll(this);
        if (newCharacter) {
            player.teleportAsync(new Location(player.getWorld(), 0, 100, 0)); // Temp
            hPlayer.getSelectedCharacter().saveCharacterPlayerData(false); // Do a first save, just in case
            MessageUtil.sendMessage(player, "<green>Character created! Welcome to Erethon!");
        }
    }

    public void onCharacterLeftClick(CharacterDisplay display) {
        if (playerIsDone) {
            return;
        }
        leaveCharacterSelection(false);
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 9999, 1, true, false, false));
        HCharacter character = display.getCharacter();
        plugin.getDatabaseManager().getHPlayer(player).setSelectedCharacter(character, true);
        playerIsDone = true;
        MessageUtil.sendMessage(player, "<green>Selected character " + character.getCharacterID());
        PlayerSelectedCharacterEvent event = new PlayerSelectedCharacterEvent(hPlayer, character, false);
        Bukkit.getPluginManager().callEvent(event);
        HandlerList.unregisterAll(this);
    }

    public void onCharacterRightClick(CharacterDisplay display) {

    }

    @EventHandler
    private void onEntityInteract(PlayerInteractEntityEvent event) {
        if (playerIsDone) {
            return;
        }
        if (event.getPlayer() != player) {
            return;
        }
        if (event.getRightClicked() instanceof Interaction interaction) {
            for (int i = 0; i < interactions.length; i++) {
                if (interactions[i] == interaction) {
                    if (i < displayedCharacters.size()) {
                        return; // the interaction is in the wrong place somehow
                    }
                    if (i >= hPlayer.getMaximumCharacters()) {
                        MessageUtil.sendMessage(player, "<gold>This character slot is currently locked!");
                        MessageUtil.sendMessage(player, "<gray><italic>You can unlock more slots through gameplay or");
                        MessageUtil.sendMessage(player, "<gray><italic>by purchasing them at <gold>store.erethon.net");
                        return; // the slot is locked
                    }
                    if (i >= displayedCharacters.size()) {
                        if (!confirmed) {
                            MessageUtil.sendMessage(player, "<red>This character slot is empty.");
                            MessageUtil.sendMessage(player, "<gray>Click again to create a new character.");
                            confirmed = true;
                            return; // the slot is empty
                        }
                        HCharacter newCharacter = new HCharacter(UUID.randomUUID(), hPlayer, 1, "default", new Timestamp(System.currentTimeMillis()), new ArrayList<>());
                        hPlayer.getCharacters().add(newCharacter);
                        playerIsDone = true;
                        newCharacter.saveToDatabase(databaseManager).thenAccept(v -> {
                            hPlayer.setSelectedCharacter(newCharacter, true);
                            player.showTitle(Title.title(Component.empty(), Component.text("Initializing new character...", NamedTextColor.GREEN)));
                            BukkitRunnable runLater = new BukkitRunnable() {
                                @Override
                                public void run() {
                                    leaveCharacterSelection(true);
                                    PlayerSelectedCharacterEvent event = new PlayerSelectedCharacterEvent(hPlayer, newCharacter, true);
                                    Bukkit.getPluginManager().callEvent(event);
                                }
                            };
                            runLater.runTaskLater(plugin, 20);
                        });
                    }
                }
            }
        }
    }

    @EventHandler
    private void onPreCommand(PlayerCommandPreprocessEvent event) {
        if (playerIsDone) {
            return;
        }
        if (event.getPlayer() != player) {
            return;
        }
        String message = event.getMessage();
        if (message.startsWith("/")) {
            event.setCancelled(true);
            MessageUtil.sendMessage(player, "<red>You can't use commands while in character selection.");
        }
    }

    private Interaction spawnInteractionEntity(Location location) {
        Interaction interaction = location.getWorld().spawn(location, Interaction.class, interactionEntity -> {
            interactionEntity.setInteractionHeight(1);
            interactionEntity.setInteractionWidth(1);
        });
        player.showEntity(plugin, interaction);
        return interaction;
    }

    private void spawnEmptySlotDisplay(Location location) {
        Location locCopy = location.clone();
        locCopy.setYaw(0);
        locCopy.setPitch(0);
        TextDisplay display = locCopy.getWorld().spawn(locCopy, TextDisplay.class, textDisplay -> {
            textDisplay.setVisibleByDefault(false);
            Component text = Component.text("+", NamedTextColor.GREEN).decorate(TextDecoration.BOLD);
            textDisplay.text(text);
            textDisplay.setBillboard(Display.Billboard.VERTICAL);
            textDisplay.setDefaultBackground(false);
            textDisplay.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0));
            Transformation transformation = new Transformation(new Vector3f(0f), new AxisAngle4f(), new Vector3f(5f), new AxisAngle4f());
            textDisplay.setTransformation(transformation);
        });
        player.showEntity(plugin, display);
        emptySlotDisplays.add(display);
    }

    private void spawnLockedSlotDisplay(Location location) {
        Location locCopy = location.clone();
        locCopy.setYaw(0);
        locCopy.setPitch(0);
        TextDisplay display = locCopy.getWorld().spawn(locCopy, TextDisplay.class, textDisplay -> {
            textDisplay.setVisibleByDefault(false);
            Component text = Component.text("\uD83D\uDD12", NamedTextColor.GOLD);
            textDisplay.text(text);
            textDisplay.setBillboard(Display.Billboard.VERTICAL);
            textDisplay.setDefaultBackground(false);
            textDisplay.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0));
            Transformation transformation = new Transformation(new Vector3f(0f), new AxisAngle4f(), new Vector3f(5f), new AxisAngle4f());
            textDisplay.setTransformation(transformation);
        });
        player.showEntity(plugin, display);
        emptySlotDisplays.add(display);
    }

    private void spawnCharacterInfoDisplay(HCharacter character, Location location) {
        Location locCopy = location.clone();
        locCopy.setYaw(0);
        locCopy.setPitch(0);
        locCopy.setY(locCopy.getY() + 2);
        TextDisplay display = locCopy.getWorld().spawn(locCopy, TextDisplay.class, textDisplay -> {
            textDisplay.setVisibleByDefault(false);
            Component text = Component.text(character.getClassId(), NamedTextColor.GOLD);
            text = text.append(Component.newline());
            text = text.append(Component.text("Level: " + character.getLevel(), NamedTextColor.GRAY));
            text = text.append(Component.newline());
            String createdAt = character.getCreatedAt().toString();
            text = text.append(Component.newline());
            text = text.append(Component.text("created at: " + createdAt, NamedTextColor.DARK_GRAY));
            if (player.hasPermission("hecate.admin")) { // Allow admins to see the character ID
                text = text.append(Component.newline());
                text = text.append(Component.text("ID: " + character.getCharacterID(), NamedTextColor.DARK_GRAY));
            }
            textDisplay.text(text);
            textDisplay.setBillboard(Display.Billboard.VERTICAL);
            textDisplay.setDefaultBackground(false);
            textDisplay.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0));
            Transformation transformation = new Transformation(new Vector3f(0f), new AxisAngle4f(), new Vector3f(0.5f), new AxisAngle4f());
            textDisplay.setTransformation(transformation);
        });
        player.showEntity(plugin, display);
        emptySlotDisplays.add(display);
    }
}
