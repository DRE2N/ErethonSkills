package de.erethon.hecate.charselection;

import de.erethon.hecate.Hecate;
import de.erethon.hecate.classes.HClass;
import de.erethon.hecate.data.HCharacter;
import de.erethon.hecate.data.HPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.List;

public class ClassSelection extends BaseSelection {

    private Hecate plugin = Hecate.getInstance();
    private HCharacter character;
    private CharacterLobby lobby;

    public ClassSelection(Player player, CharacterLobby lobby) {
        super(player);
        HPlayer hPlayer = plugin.getDatabaseManager().getHPlayer(player);
        this.lobby = lobby;
        if (hPlayer == null) {
            player.sendMessage(Component.translatable("hecate.data.player_not_found"));
            return;
        }
        character = hPlayer.getSelectedCharacter();
        if (character == null) {
            player.sendMessage(Component.translatable("hecate.commands.character.no_character_selected"));
            return;
        }
        if (character.getHClass() != null) {
            player.sendMessage(Component.translatable("hecate.class.already_has_class"));
            return;
        }
        setup();
    }

    @Override
    protected void setup() {
        // Clear any existing displays from character selection
        for (TextDisplay display : emptySlotDisplays) {
            display.remove();
        }
        emptySlotDisplays.clear();

        // Show welcome message
        player.sendMessage(Component.text("═══════════════════════════════════════", NamedTextColor.GOLD));
        player.sendMessage(Component.translatable("hecate.class.selection.title"));
        player.sendMessage(Component.text("═══════════════════════════════════════", NamedTextColor.GOLD));
        player.sendMessage(Component.translatable("hecate.class.selection.choose_starting"));
        player.sendMessage(Component.translatable("hecate.class.selection.left_click_info"));
        player.sendMessage(Component.translatable("hecate.class.selection.right_click_select"));

        for (HClass hClass : plugin.getHClasses()) {
            ClassDisplay display = new ClassDisplay(this, hClass);
            displayed.add(display);
        }
        List<Location> locations = lobby.getPedestalLocations();
        if (locations.size() < displayed.size()) {
            player.sendMessage(Component.translatable("hecate.class.selection.not_enough_pedestals"));
        }

        for (int i = 0; i < Math.min(displayed.size(), locations.size()); i++) {
            displayed.get(i).display(player, locations.get(i));
            spawnClassInfoDisplay(((ClassDisplay) displayed.get(i)).getHClass(), locations.get(i));
        }
    }

    @Override
    public void onRightClick(BaseDisplay display) {
        if (playerIsDone) return;

        if (!(display instanceof ClassDisplay classDisplay)) {
            return;
        }
        if (classDisplay.getHClass() == null) {
            player.sendMessage(Component.translatable("hecate.class.not_found"));
            return;
        }

        HClass selectedClass = classDisplay.getHClass();
        character.setHClass(selectedClass);
        character.setTraitline(selectedClass.getStarterTraitline());

        // Visual feedback
        player.showTitle(Title.title(
            Component.translatable("hecate.class.selected.title"),
                selectedClass.getDisplayName()
        ));

        player.sendMessage(Component.text("═══════════════════════════════════════", NamedTextColor.GOLD));
        player.sendMessage(Component.translatable("hecate.class.selected.success_title"));
        player.sendMessage(Component.translatable("hecate.class.selected.class_label",
                selectedClass.getDisplayName()));
        player.sendMessage(Component.translatable("hecate.class.selected.traitline_label", selectedClass.getDisplayName()));
        player.sendMessage(Component.text("═══════════════════════════════════════", NamedTextColor.GOLD));
        player.sendMessage(Component.translatable("hecate.class.selected.welcome"));

        // Clean up displays - both character displays and text displays
        for (BaseDisplay baseDisplay : displayed) {
            baseDisplay.remove(player);
        }
        for (TextDisplay textDisplay : emptySlotDisplays) {
            textDisplay.remove();
        }
        emptySlotDisplays.clear();

        playerIsDone = true;
        done();

        // Save the character to database with the new class and traitline, then teleport
        character.saveToDatabase().thenAccept(v -> {
            player.teleportAsync(new Location(player.getWorld(), 0, 100, 0)); // TODO: Replace with proper spawn location
            character.saveCharacterPlayerData(false);
        }).exceptionally(ex -> {
            player.sendMessage(Component.translatable("hecate.data.error_saving", Component.text(ex.getMessage())));
            ex.printStackTrace();
            return null;
        });
    }

    @Override
    public void onLeftClick(BaseDisplay display) {
        if (playerIsDone) return;

        if (!(display instanceof ClassDisplay classDisplay)) {
            return;
        }
        if (classDisplay.getHClass() == null) {
            player.sendMessage(Component.translatable("hecate.class.not_found"));
            return;
        }

        HClass cl = classDisplay.getHClass();
        player.sendMessage(Component.text("═══════════════════════════════════════", NamedTextColor.GOLD));
        player.sendMessage(cl.getDisplayName());
        player.sendMessage(cl.getDescription());
        player.sendMessage(Component.translatable("hecate.class.selected.traitline_label",
            Component.text(cl.getStarterTraitline().getName())));
        player.sendMessage(Component.text("═══════════════════════════════════════", NamedTextColor.GOLD));
        player.sendMessage(Component.translatable("hecate.class.selection.right_click_to_select"));
    }

    private void spawnClassInfoDisplay(HClass hClass, Location location) {
        Location locCopy = location.clone();
        locCopy.setYaw(0);
        locCopy.setPitch(0);
        locCopy.setY(locCopy.getY() + 2.5);
        TextDisplay display = locCopy.getWorld().spawn(locCopy, TextDisplay.class, textDisplay -> {
            textDisplay.setVisibleByDefault(false);
            Component text = hClass.getDisplayName();
            text = text.append(Component.newline());
            text = text.append(Component.translatable("hecate.class.selection.click_learn_more"));
            textDisplay.text(text);
            textDisplay.setBillboard(Display.Billboard.VERTICAL);
            textDisplay.setDefaultBackground(false);
            textDisplay.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0));
            Transformation transformation = new Transformation(new Vector3f(0f), new AxisAngle4f(), new Vector3f(0.7f), new AxisAngle4f());
            textDisplay.setTransformation(transformation);
        });
        player.showEntity(plugin, display);
        emptySlotDisplays.add(display);
    }

}
