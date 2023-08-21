package de.erethon.hecate.casting;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.config.EConfig;
import de.erethon.bedrock.user.LoadableUser;
import de.erethon.hecate.Hecate;
import de.erethon.hecate.arenas.ArenaPlayer;
import de.erethon.papyrus.PlayerSwitchProfileEvent;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class HPlayer  extends EConfig implements LoadableUser, Listener {

    public static final int CONFIG_VERSION = 1;

    private Player player;
    private ArenaPlayer arenaPlayer;
    private HCharacter selectedCharacter;
    private final HashMap<Integer, HCharacter> characters = new HashMap<>();
    private int profileID;

    private boolean autoJoinWithLastCharacter = false;

    public HPlayer(Player player) {
        super(HPlayerCache.getPlayerFile(player), CONFIG_VERSION);
        this.player = player;
        load();
        if (characters.isEmpty()) {
            characters.put(0, new HCharacter(this, 0));
        }
        selectedCharacter = characters.get(0);
        Bukkit.getPluginManager().registerEvents(this, Hecate.getInstance());
    }

    public void switchCharacterTo(int id) {
        saveCharacter(profileID);
        profileID = id;
        if (characters.get(id) == null) {
            characters.put(id, new HCharacter(this, id));
            return;
        }
        selectedCharacter = characters.get(id);
    }

    public HCharacter loadCharacter(int id) {
        HCharacter character = new HCharacter(this, id);
        SpellbookAPI spellbook = Bukkit.getServer().getSpellbookAPI();
        for (String spellId : config.getStringList("characters." + id + ".unlockedSpells")) {
            SpellData spellData = spellbook.getLibrary().getSpellByID(spellId);
            if (spellData == null) {
                if (!spellId.equals("empty")) MessageUtil.log("Unknown spell '" + spellId + "' found under 'unlockedSlots'");
                continue;
            }
            character.addSpell(spellData);
        }
        List<String> slotList = config.getStringList("characters." + id + ".assignedSlots");
        if (slotList.size() > 8) {
            MessageUtil.log("Illegal amount of slots assigned");
            return null;
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
        character.setLevel(config.getInt("characters." + id + ".level", 1));
        character.setXp(config.getInt("characters." + id + ".xp", 0));
        character.setMaxEnergy(config.getInt("characters." + id + ".maxEnergy", 100));
        character.setEnergy(config.getInt("characters." + id + ".energy", 50));
        character.sethClass(Hecate.getInstance().getHClass(config.getString("characters" + id + ".class", "assassin")));
        return character;
    }

    @Override
    public void load() {
        autoJoinWithLastCharacter = config.getBoolean("autoJoinWithLastCharacter", false);
        profileID = config.getInt("profileID", 0);
        Set<String> characterList = config.getConfigurationSection("characters").getKeys(false);
        for (String id : characterList) {
            int i = Integer.parseInt(id);
            characters.put(i, loadCharacter(i));
            if (i == profileID) {
                selectedCharacter = loadCharacter(i);
            }
        }
        arenaPlayer = new ArenaPlayer(this, player, config.getConfigurationSection("arenas"));
        MessageUtil.log("Loaded " + characterList.size() + " characters for " + player.getName() + "! Last selected: " + profileID);
    }

    /* LoadableUser methods */

    @Override
    public void updatePlayer(Player player) {
        this.player = player;
    }

    @Override
    public void saveUser() {
        MessageUtil.log("User " + player.getName() + " saved!");
        for (HCharacter character : characters.values()) {
            saveCharacter(character.getCharacterID());
        }
        config.set("autoJoinWithLastCharacter", autoJoinWithLastCharacter);
        config.set("profileID", profileID);
        config.set("arenas", arenaPlayer.save());
        save();
    }

    public void saveCharacter(int id) {
        config.set("characters." + id + ".unlockedSpells", selectedCharacter.getUnlockedSpells().stream().map(SpellData::getId).collect(Collectors.toList()));
        config.set("characters." + id + ".assignedSlots", Arrays.stream(selectedCharacter.getAssignedSlots()).map(s -> s == null ? "empty" : s.getId()).collect(Collectors.toList()));
        config.set("characters." + id + ".level", selectedCharacter.getLevel());
        config.set("characters." + id + ".xp", selectedCharacter.getXp());
        config.set("characters." + id + ".maxEnergy", selectedCharacter.getMaxEnergy());
        config.set("characters." + id + ".energy", selectedCharacter.getEnergy());
        if (selectedCharacter.gethClass() == null) {
            config.set("characters." + id + ".class", "assassin");
            return;
        }
        config.set("characters." + id + ".class", selectedCharacter.gethClass().getId());
    }

    public Player getPlayer() {
        return player;
    }

    public HCharacter getSelectedCharacter() {
        if (selectedCharacter == null) {
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
        MessageUtil.log("aaaa");
        if (event.getPlayer().getUniqueId() != player.getUniqueId()) return;
        MessageUtil.sendMessage(player,"Switching profile to " + event.getNewProfileID() + "...");
        switchCharacterTo(event.getNewProfileID());
        saveUser();
    }

    public Collection<HCharacter> getCharacters() {
        return characters.values();
    }

    public boolean isAutoJoinWithLastCharacter() {
        return autoJoinWithLastCharacter;
    }
}
