package de.erethon.hecate.charselection;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.hecate.Hecate;
import de.erethon.hecate.classes.HClass;
import de.erethon.hecate.data.HCharacter;
import de.erethon.hecate.data.HPlayer;
import org.bukkit.Location;
import org.bukkit.entity.Player;

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
            MessageUtil.sendMessage(player, "<red>Player not found.");
            return;
        }
        character = hPlayer.getSelectedCharacter();
        if (character == null) {
            MessageUtil.sendMessage(player, "<red>No character selected. Cannot display class selection.");
            return;
        }
        if (character.getHClass() != null) {
            MessageUtil.sendMessage(player, "<red>You already have a class. This shouldn't be possible.");
            return;
        }
        setup();
    }

    @Override
    protected void setup() {
        for (HClass hClass : plugin.getHClasses()) {
            ClassDisplay display = new ClassDisplay(this, hClass);
            displayed.add(display);
        }
        List<Location> locations = lobby.getPedestalLocations();
        for (int i = 0; i < displayed.size(); i++) {
            if (i >= locations.size()) {
                break;
            }
            displayed.get(i).display(player, locations.get(i));
        }
        MessageUtil.sendMessage(player, "<green>Right click a class to select it. Left click to learn more about a class.");
    }

    @Override
    public void onRightClick(BaseDisplay display) {
        if (!(display instanceof ClassDisplay classDisplay)) {
            return;
        }
        if (classDisplay.getHClass() == null) {
            MessageUtil.sendMessage(player, "<red>Class not found.");
            return;
        }
        HClass cl = classDisplay.getHClass();
        character.setHClass(cl);
        character.setTraitline(cl.getStarterTraitline());
        player.teleportAsync(new Location(player.getWorld(), 0, 100, 0)); // TODO: Temp
        character.getHPlayer().getSelectedCharacter().saveCharacterPlayerData(false); // Do a save, just in case
        MessageUtil.sendMessage(player, "<green><i>Character created! Welcome to Erethon!");

    }

    @Override
    public void onLeftClick(BaseDisplay display) {
        if (!(display instanceof ClassDisplay classDisplay)) {
            return;
        }
        if (classDisplay.getHClass() == null) {
            MessageUtil.sendMessage(player, "<red>Class not found.");
            return;
        }
        HClass cl = classDisplay.getHClass();
        MessageUtil.sendMessage(player, "<green>Class: " + cl.getName());
        MessageUtil.sendMessage(player, "<green>Description: " + cl.getDescription());
        MessageUtil.sendMessage(player, "<green>Starting Traitline: " + cl.getStarterTraitline().getName());
    }

}
