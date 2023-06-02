package de.erethon.hecate.classes;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.hecate.Hecate;
import de.erethon.spellbook.api.SpellbookAPI;
import de.erethon.spellbook.api.TraitData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Traitline extends YamlConfiguration {

    SpellbookAPI spellbookAPI = SpellbookAPI.getInstance();
    MiniMessage mm = MiniMessage.miniMessage();
    private String id;
    private Component displayName;
    private final List<Component> description = new ArrayList<>();
    private int inactiveModelData = 0;
    private int activeModelData = 0;
    private int initialLevelRequirement = 0;
    private final HashMap<Integer, List<TraitLineEntry>> traitMap = new HashMap<>();

    public Traitline(File file) throws IOException, InvalidConfigurationException {
        load(file);
    }

    public String getId() {
        return id;
    }

    public Component getDisplayName() {
        return displayName;
    }

    public List<Component> getDescription() {
        return description;
    }

    public int getInitialLevelRequirement() {
        return initialLevelRequirement;
    }

    public HashMap<Integer, List<TraitLineEntry>> getTraitMap() {
        return traitMap;
    }

    public List<TraitLineEntry> getTraitLineEntries(int level) {
        return traitMap.get(level);
    }

    @Override
    public void load(File file) throws IOException, InvalidConfigurationException {
        super.load(file);
        id = file.getName().replace(".yml", "");
        displayName = mm.deserialize(getString("displayName", "<red>ERROR"));
        description.add(mm.deserialize(getString("description", "")));
        inactiveModelData = getInt("inactiveModelData", 0);
        activeModelData = getInt("activeModelData", 0);
        initialLevelRequirement = getInt("initialLevelRequirement", 0);
        for (String key : getConfigurationSection("traitLine").getKeys(false)) {
            int level = Integer.parseInt(key);
            List<TraitLineEntry> traits = new ArrayList<>();
            for (String traitId : getConfigurationSection("traitLine." + level).getKeys(false)) {
                ConfigurationSection traitSection = getConfigurationSection("traitLine." + level + "." + traitId);
                TraitData traitData = spellbookAPI.getLibrary().getTraitByID(traitId);
                if (traitData == null) {
                    System.out.println("Unknown trait '" + traitId + "' found in traitline file " + getName());
                    continue;
                }
                int levelRequirement = traitSection.getInt("levelRequirement", 0);
                int cost = traitSection.getInt("cost", 0);
                boolean combatOnly = traitSection.getBoolean("combatOnly", false);
                int activeModelData = traitSection.getInt("activeModelData", 0);
                int inactiveModelData = traitSection.getInt("inactiveModelData", 0);
                TraitLineEntry traitLineEntry = new TraitLineEntry(traitData, levelRequirement, cost, combatOnly, activeModelData, inactiveModelData);
                traits.add(traitLineEntry);
            }
            traitMap.put(level, traits);
        }
        MessageUtil.log("Loaded traitline " + id + " from " + file.getName() + " with " + traitMap.size() + " levels and " + traitMap.values().stream().mapToInt(List::size).sum() + " traits.");
    }
}
