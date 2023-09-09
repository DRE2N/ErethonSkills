package de.erethon.hecate.casting;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.hecate.Hecate;
import de.erethon.hecate.arenas.ArenaPlayer;
import de.erethon.hecate.classes.HClass;
import de.erethon.hecate.classes.Traitline;
import de.erethon.papyrus.PlayerSwitchProfileEvent;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookAPI;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class HPlayer extends YamlConfiguration implements Listener {

    private static final HClass DEFAULT_CLASS = Hecate.getInstance().getHClass("Assassin");

    private final File file;
    private final Player player;
    private ArenaPlayer arenaPlayer = null;
    private HCharacter selectedCharacter = null;
    private final List<HCharacter> characters = new ArrayList<>();
    private int profileID = 0;

    private boolean autoJoinWithLastCharacter = true;

    public HPlayer(Player player) {
        super();
        MessageUtil.log("Creating new HPlayer for " + player.getName() + "...");
        this.file = new File(Hecate.getInstance().getDataFolder() + "/players/" + player.getUniqueId() + ".yml");
        try {
            file.createNewFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.player = player;
        try {
            load(file);
        } catch (IOException | InvalidConfigurationException e) {
            throw new RuntimeException(e);
        }
        loadUser();
        if (characters.isEmpty()) {
            MessageUtil.log("No characters found for " + player.getName() + ". Creating new default character...");
            characters.add(new HCharacter(this, 0));
        }
        Bukkit.getPluginManager().registerEvents(this, Hecate.getInstance());
    }

    public void switchCharacterTo(int id) {
        if (selectedCharacter != null) {
            saveCharacter(profileID);
        }
        profileID = id;
        if (characters.size() <= id) {
            characters.add(new HCharacter(this, id));
        }
        selectedCharacter = characters.get(id);
    }

    public HCharacter loadCharacter(int id) {
        HCharacter character = new HCharacter(this, id);
        SpellbookAPI spellbook = Bukkit.getServer().getSpellbookAPI();
        for (String spellId : getStringList("characters." + id + ".unlockedSpells")) {
            SpellData spellData = spellbook.getLibrary().getSpellByID(spellId);
            if (spellData == null) {
                if (!spellId.equals("empty")) MessageUtil.log("Unknown spell '" + spellId + "' found under 'unlockedSlots'");
                continue;
            }
            character.addSpell(spellData);
        }
        List<String> slotList = getStringList("characters." + id + ".assignedSlots");
        if (slotList.size() > 8) {
            MessageUtil.log("Illegal amount of slots assigned");
        }
        for (int i = 0; i < slotList.size(); i++) {
            String spellId = slotList.get(i);
            SpellData spellData = spellbook.getLibrary().getSpellByID(spellId);
            if (spellData == null) {
                if (!spellId.equals("empty")) MessageUtil.log("Unknown spell '" + spellId + "' found under 'assignedSlots'");
                continue;
            }
            character.setSpellAt(i, spellData);
        }
        character.setLevel(getInt("characters." + id + ".level", 1));
        character.setXp(getInt("characters." + id + ".xp", 0));
        character.setMaxEnergy(getInt("characters." + id + ".maxEnergy", 100));
        character.setEnergy(getInt("characters." + id + ".energy", 50));
        character.sethClass(Hecate.getInstance().getHClass(getString("characters" + id + ".class", DEFAULT_CLASS.getId())));
        Traitline traitline = Hecate.getInstance().getTraitline(getString("characters." + id + ".traitline", character.gethClass().getDefaultTraitline().getId()));
        character.setSelectedTraitline(traitline);
        return character;
    }

    public void loadUser() {
        autoJoinWithLastCharacter = getBoolean("autoJoinWithLastCharacter", false);
        profileID = getInt("profileID", 0);
        ConfigurationSection charSection = getConfigurationSection("characters");
        if (charSection == null) {
            MessageUtil.log("No characters found for " + player.getName() + ". New player?");
            return;
        }
        Set<String> characterList = charSection.getKeys(false);
        for (String id : characterList) {
            int i = Integer.parseInt(id);
            characters.add(loadCharacter(i));
            if (i == profileID) {
                selectedCharacter = characters.get(i);
            }
        }
        arenaPlayer = new ArenaPlayer(this, player,getConfigurationSection("arenas"));
        MessageUtil.log("Loaded " + characterList.size() + " characters for " + player.getName() + "! Last selected: " + profileID);
    }

    public void saveUser() {
        MessageUtil.log("User " + player.getName() + " saved!");
        for (HCharacter character : characters) {
            try { // Let's not completely fail just because one character fails
                saveCharacter(character.getCharacterID());
            } catch (Exception e) {
                MessageUtil.log("Failed to save character for " + player.getName());
                e.printStackTrace();
            }
        }
       set("autoJoinWithLastCharacter", autoJoinWithLastCharacter);
       set("profileID", profileID);
        if (arenaPlayer != null) {
            set("arenas", arenaPlayer.save());
        }
        try {
            save(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        MessageUtil.log("Saved " + characters.size() + " characters for " + player.getName() + "! Last selected: " + profileID);
    }

    public void saveCharacter(int id) {
        set("characters." + id + ".unlockedSpells", selectedCharacter.getUnlockedSpells().stream().map(SpellData::getId).collect(Collectors.toList()));
        set("characters." + id + ".assignedSlots", Arrays.stream(selectedCharacter.getAssignedSlots()).map(s -> s == null ? "empty" : s.getId()).collect(Collectors.toList()));
        set("characters." + id + ".level", selectedCharacter.getLevel());
        set("characters." + id + ".xp", selectedCharacter.getXp());
        set("characters." + id + ".maxEnergy", selectedCharacter.getMaxEnergy());
        set("characters." + id + ".energy", selectedCharacter.getEnergy());
        if (selectedCharacter.gethClass() == null) {
            set("characters." + id + ".class", "assassin");
            return;
        }
        set("characters." + id + ".class", selectedCharacter.gethClass().getId());
        set("characters." + id + ".traitline", selectedCharacter.getSelectedTraitline().getId());
    }

    public Player getPlayer() {
        return player;
    }

    public HCharacter getSelectedCharacter() {
        if (selectedCharacter == null) {
            if (characters.get(0) == null) {
                characters.add(new HCharacter(this, 0));
            }
            selectedCharacter = characters.get(0);
            return selectedCharacter;
        }
        return selectedCharacter;
    }

    public int getSelectedCharacterID() {
        return profileID;
    }

    @EventHandler
    public void onSwitch(PlayerSwitchProfileEvent event) {
        if (event.getPlayer().getUniqueId() != player.getUniqueId()) return;
        MessageUtil.sendMessage(player,"Switching profile to " + event.getNewProfileID() + "...");
        switchCharacterTo(event.getNewProfileID());
        selectedCharacter.setClassAttributes(selectedCharacter.gethClass());
        saveUser();
    }

    public List<HCharacter> getCharacters() {
        return characters;
    }

    public boolean isAutoJoinWithLastCharacter() {
        return autoJoinWithLastCharacter;
    }
}
