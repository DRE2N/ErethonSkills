package de.erethon.hecate.charselection;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.hecate.data.HCharacter;
import de.erethon.hecate.data.HPlayer;
import de.erethon.hecate.data.dao.CharacterDao;
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
import org.bukkit.event.player.PlayerInteractEntityEvent;
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

public class CharacterSelection extends BaseSelection {

    private final HPlayer hPlayer;
    private final CharacterLobby lobby;
    private final Set<TextDisplay> emptySlotDisplays = new HashSet<>();
    private final Interaction[] interactions = new Interaction[9];
    private boolean confirmed = false;
    private boolean playerIsDone = false;
    private UUID characterToDelete = null; // Track which character is pending deletion
    private long deleteRequestTime = 0; // Track when delete was requested

    public CharacterSelection(Player player, CharacterLobby lobby) {
        super(player);
        this.lobby = lobby;
        this.hPlayer = plugin.getDatabaseManager().getHPlayer(player);
        if (hPlayer.getSelectedCharacter() != null && hPlayer.getSelectedCharacter().isInCastMode()) {
            player.sendMessage(Component.translatable("hecate.character.selection.combat_mode_error"));
            return;
        }
        List<HCharacter> characters = hPlayer.getCharacters();
        for (HCharacter character : characters) {
            CharacterDisplay display = new CharacterDisplay(character, this);
            displayed.add(display);
        }
        if (hPlayer.getSelectedCharacter() == null) {
            setup();
            return;
        }
        hPlayer.getSelectedCharacter().saveToDatabase().thenAccept(v -> { // make sure the current character is saved first
            setup();
            hPlayer.setSelectedCharacter(null, false);
        });

    }

    protected void setup() {
        player.teleportAsync(lobby.getOrigin()).thenAccept(a -> {
            List<Location> locations = lobby.getPedestalLocations();
            for (int i = 0; i < locations.size(); i++) {
                if (i >= hPlayer.getMaximumCharacters()) {
                    spawnLockedSlotDisplay(locations.get(i));
                    Interaction interaction = spawnInteractionEntity(locations.get(i));
                    interactions[i] = interaction;
                } else if (i < displayed.size()) {
                    if (displayed.get(i) instanceof CharacterDisplay characterDisplay) {
                        characterDisplay.display(player, locations.get(i));
                        spawnCharacterInfoDisplay(characterDisplay.getCharacter(), locations.get(i));
                    }
                } else {
                    spawnEmptySlotDisplay(locations.get(i));
                    Interaction interaction = spawnInteractionEntity(locations.get(i));
                    interactions[i] = interaction;
                }
            }
        });
    }

    private void leaveCharacterSelection(boolean newCharacter) {
        for (BaseDisplay characterDisplay : displayed) {
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
        if (newCharacter) {
            player.showTitle(Title.title(
                Component.translatable("hecate.character.selection.creating_new", NamedTextColor.GOLD, TextDecoration.BOLD),
                Component.translatable("hecate.character.selection.choose_class")
            ));
            player.sendMessage(Component.translatable("hecate.character.selection.creating_new"));
            player.sendMessage(Component.translatable("hecate.character.selection.choose_class"));

            // Delay class selection to allow for proper cleanup
            new BukkitRunnable() {
                @Override
                public void run() {
                    ClassSelection classSelection = new ClassSelection(player, lobby);
                }
            }.runTaskLater(plugin, 5L);
        }
        done();
    }

    public void onLeftClick(BaseDisplay display) {
        if (playerIsDone) {
            return;
        }
        if (!(display instanceof CharacterDisplay characterDisplay)) {
            return;
        }
        leaveCharacterSelection(false);
        HCharacter character = characterDisplay.getCharacter();
        plugin.getDatabaseManager().getHPlayer(player).setSelectedCharacter(character, true);
        playerIsDone = true;
        MessageUtil.sendMessage(player, "<green>Selected character " + character.getCharacterID());
        PlayerSelectedCharacterEvent event = new PlayerSelectedCharacterEvent(hPlayer, character, false);
        Bukkit.getPluginManager().callEvent(event);
        HandlerList.unregisterAll(this);
    }

    public void onRightClick(BaseDisplay display) {
        // Regular right-click - could be used for character info or other features
    }

    public void onSneakRightClick(BaseDisplay display) {
        if (playerIsDone) {
            return;
        }
        if (!(display instanceof CharacterDisplay characterDisplay)) {
            return;
        }

        HCharacter character = characterDisplay.getCharacter();
        UUID characterId = character.getCharacterID();
        long currentTime = System.currentTimeMillis();

        // Check if this is a confirmation for the same character within 10 seconds
        if (characterToDelete != null && characterToDelete.equals(characterId) &&
            (currentTime - deleteRequestTime) < 10000) {

            // Proceed with deletion
            performCharacterDeletion(character, characterDisplay);

        } else {
            // First click - request confirmation
            characterToDelete = characterId;
            deleteRequestTime = currentTime;

            Component className = character.getHClass() != null ?
                    character.getHClass().getDisplayName() :
                Component.translatable("hecate.misc.no_class", NamedTextColor.GRAY);

            player.sendMessage(Component.translatable("hecate.character.deletion.warning_title"));
            player.sendMessage(Component.translatable("hecate.character.deletion.warning_about_to_delete",
                className,
                Component.text(character.getLevel())));
            player.sendMessage(Component.translatable("hecate.character.deletion.warning_irreversible"));
            player.sendMessage(Component.translatable("hecate.character.deletion.warning_confirm"));
            player.sendMessage(Component.translatable("hecate.character.deletion.warning_cancel"));

            player.showTitle(Title.title(
                Component.translatable("hecate.character.deletion.title_delete"),
                Component.translatable("hecate.character.deletion.title_confirm")
            ));
        }
    }

    private void performCharacterDeletion(HCharacter character, CharacterDisplay characterDisplay) {
        Component className = character.getHClass().getDisplayName();

        // Find the index of the character being deleted
        int deletedIndex = -1;
        for (int i = 0; i < displayed.size(); i++) {
            if (displayed.get(i) instanceof CharacterDisplay cd &&
                cd.getCharacter().getCharacterID().equals(character.getCharacterID())) {
                deletedIndex = i;
                break;
            }
        }

        // Remove from display and player's character list
        displayed.remove(characterDisplay);
        characterDisplay.remove(player);
        hPlayer.getCharacters().remove(character);

        // If this was the selected character, clear it
        if (hPlayer.getSelectedCharacter() != null &&
            hPlayer.getSelectedCharacter().getCharacterID().equals(character.getCharacterID())) {
            hPlayer.setSelectedCharacter(null, true);
        }

        // Delete from database
        plugin.getDatabaseManager().executeAsync(handle -> {
            handle.attach(CharacterDao.class).deleteCharacter(character.getCharacterID());
        }).thenRun(() -> {
            player.sendMessage(Component.translatable("hecate.character.deletion.success"));
            player.sendMessage(Component.translatable("hecate.character.deletion.success_detail",
                className, Component.text(character.getLevel())));
        }).exceptionally(ex -> {
            player.sendMessage(Component.translatable("hecate.data.error_saving", Component.text(ex.getMessage())));
            ex.printStackTrace();
            return null;
        });

        // Clear deletion tracking
        characterToDelete = null;
        deleteRequestTime = 0;

        // Visual feedback
        player.showTitle(Title.title(
            Component.translatable("hecate.character.deletion.title_deleted"),
            Component.translatable("hecate.character.deletion.title_removed", className)
        ));

        // Completely rebuild the display layout to shift characters left
        rebuildDisplayLayout();
    }

    private void rebuildDisplayLayout() {
        // Clear all existing displays and interactions
        for (TextDisplay display : emptySlotDisplays) {
            display.remove();
        }
        emptySlotDisplays.clear();

        // Clear all interactions except those for existing characters
        for (int i = 0; i < interactions.length; i++) {
            if (interactions[i] != null && i >= displayed.size()) {
                interactions[i].remove();
                interactions[i] = null;
            }
        }

        // Remove all existing character displays to reposition them
        for (BaseDisplay characterDisplay : displayed) {
            characterDisplay.remove(player);
        }

        // Rebuild the entire layout from scratch
        List<Location> locations = lobby.getPedestalLocations();
        for (int i = 0; i < locations.size(); i++) {
            if (i >= hPlayer.getMaximumCharacters()) {
                // Locked slot
                spawnLockedSlotDisplay(locations.get(i));
                if (interactions[i] == null) {
                    interactions[i] = spawnInteractionEntity(locations.get(i));
                }
            } else if (i < displayed.size()) {
                // Character slot - redisplay the character at the new position
                if (displayed.get(i) instanceof CharacterDisplay characterDisplay) {
                    characterDisplay.display(player, locations.get(i));
                    spawnCharacterInfoDisplay(characterDisplay.getCharacter(), locations.get(i));
                }
            } else {
                // Empty slot
                spawnEmptySlotDisplay(locations.get(i));
                if (interactions[i] == null) {
                    interactions[i] = spawnInteractionEntity(locations.get(i));
                }
            }
        }
    }

    private void spawnCharacterInfoDisplay(HCharacter character, Location location) {
        Location locCopy = location.clone();
        locCopy.setYaw(0);
        locCopy.setPitch(0);
        locCopy.setY(locCopy.getY() + 2);
        TextDisplay display = locCopy.getWorld().spawn(locCopy, TextDisplay.class, textDisplay -> {
            textDisplay.setVisibleByDefault(false);

            Component text = character.getHClass() != null ?
                character.getHClass().getDisplayName() :
                Component.translatable("hecate.misc.no_class", NamedTextColor.GRAY);
            text = text.append(Component.newline());
            text = text.append(Component.translatable("hecate.character.info.level", Component.text(character.getLevel())));
            text = text.append(Component.newline());
            text = text.append(Component.newline());
            text = text.append(Component.translatable("hecate.character.controls.left_click_select"));
            text = text.append(Component.newline());
            text = text.append(Component.translatable("hecate.character.controls.sneak_right_click_delete"));

            String createdAt = character.getCreatedAt().toString().substring(0, 10); // Just the date
            text = text.append(Component.newline());
            text = text.append(Component.translatable("hecate.character.info.created", Component.text(createdAt)));

            if (player.hasPermission("hecate.admin")) { // Allow admins to see the character ID
                text = text.append(Component.newline());
                text = text.append(Component.text("ID: " + character.getCharacterID().toString().substring(0, 8) + "...", NamedTextColor.DARK_GRAY));
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

    private void spawnLockedSlotDisplay(Location location) {
        Location locCopy = location.clone();
        locCopy.setYaw(0);
        locCopy.setPitch(0);
        locCopy.setY(locCopy.getY() + 1.5);

        TextDisplay display = locCopy.getWorld().spawn(locCopy, TextDisplay.class, textDisplay -> {
            textDisplay.setVisibleByDefault(false);

            Component text = Component.text("🔒", NamedTextColor.RED)
                .decoration(TextDecoration.BOLD, true)
                .append(Component.newline())
                .append(Component.translatable("hecate.character.slot.locked", NamedTextColor.GRAY));

            textDisplay.text(text);
            textDisplay.setBillboard(Display.Billboard.VERTICAL);
            textDisplay.setDefaultBackground(false);
            textDisplay.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0));

            Transformation transformation = new Transformation(
                new Vector3f(0f),
                new AxisAngle4f(),
                new Vector3f(2.0f),
                new AxisAngle4f()
            );
            textDisplay.setTransformation(transformation);
        });

        player.showEntity(plugin, display);
        emptySlotDisplays.add(display);
    }

    private void spawnEmptySlotDisplay(Location location) {
        Location locCopy = location.clone();
        locCopy.setYaw(0);
        locCopy.setPitch(0);
        locCopy.setY(locCopy.getY() + 1.5);

        TextDisplay display = locCopy.getWorld().spawn(locCopy, TextDisplay.class, textDisplay -> {
            textDisplay.setVisibleByDefault(false);

            Component text = Component.text("✚", NamedTextColor.GREEN)
                .decoration(TextDecoration.BOLD, true)
                .append(Component.newline())
                .append(Component.translatable("hecate.character.slot.empty", NamedTextColor.GRAY))
                .append(Component.newline())
                .append(Component.translatable("hecate.character.slot.click_to_create", NamedTextColor.YELLOW));

            textDisplay.text(text);
            textDisplay.setBillboard(Display.Billboard.VERTICAL);
            textDisplay.setDefaultBackground(false);
            textDisplay.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0));

            Transformation transformation = new Transformation(
                new Vector3f(0f),
                new AxisAngle4f(),
                new Vector3f(2.0f),
                new AxisAngle4f()
            );
            textDisplay.setTransformation(transformation);
        });

        player.showEntity(plugin, display);
        emptySlotDisplays.add(display);
    }

    private Interaction spawnInteractionEntity(Location location) {
        Location locCopy = location.clone();
        locCopy.setY(locCopy.getY() + 0.5);

        return locCopy.getWorld().spawn(locCopy, Interaction.class, interaction -> {
            interaction.setInteractionWidth(2.0f);
            interaction.setInteractionHeight(3.0f);
            interaction.setResponsive(true);
        });
    }

    @EventHandler
    private void onEntityInteract(PlayerInteractEntityEvent event) {
        if (playerIsDone) {
            return;
        }
        if (event.getPlayer() != player) {
            return;
        }

        // Only handle main hand interactions to prevent double triggers
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) {
            return;
        }

        // Reset deletion confirmation if player interacts with something else
        if (characterToDelete != null) {
            characterToDelete = null;
            deleteRequestTime = 0;
            player.sendMessage(Component.translatable("hecate.character.deletion.cancelled"));
            player.showTitle(Title.title(Component.empty(), Component.empty()));
        }

        if (event.getRightClicked() instanceof Interaction interaction) {
            for (int i = 0; i < interactions.length; i++) {
                if (interactions[i] == interaction) {
                    if (i < displayed.size()) {
                        return; // the interaction is in the wrong place somehow
                    }
                    if (i >= hPlayer.getMaximumCharacters()) {
                        player.sendMessage(Component.translatable("hecate.character.selection.slot_locked"));
                        player.sendMessage(Component.translatable("hecate.character.selection.slot_locked_help"));
                        player.sendMessage(Component.translatable("hecate.character.selection.slot_locked_store"));
                        return; // the slot is locked
                    }
                    if (i >= displayed.size()) {
                        if (!confirmed) {
                            player.sendMessage(Component.translatable("hecate.character.selection.slot_empty"));
                            player.sendMessage(Component.translatable("hecate.character.selection.slot_empty_confirm"));
                            confirmed = true;
                            return; // the slot is empty
                        }
                        HCharacter newCharacter = new HCharacter(UUID.randomUUID(), hPlayer, 1, null, new Timestamp(System.currentTimeMillis()), new ArrayList<>());
                        hPlayer.getCharacters().add(newCharacter);
                        playerIsDone = true;
                        newCharacter.saveToDatabase().thenAccept(v -> {
                            hPlayer.setSelectedCharacter(newCharacter, true);
                            player.showTitle(Title.title(Component.empty(), Component.translatable("hecate.character.selection.initializing")));
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
}
