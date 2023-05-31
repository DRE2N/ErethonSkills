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
    private int initialLevelRequirement = 0;
    private HClass hClass;
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

    public HClass gethClass() {
        return hClass;
    }

    public HashMap<Integer, List<TraitLineEntry>> getTraitMap() {
        return traitMap;
    }

    @Override
    public void load(File file) throws IOException, InvalidConfigurationException {
        super.load(file);
        id = file.getName().replace(".yml", "");
        hClass = Hecate.getInstance().getHClass(getString("class"));
        if (id == null) {
            MessageUtil.log("Traitline " + id + " has no valid class assigned. Skipping.");
            return;
        }
        displayName = mm.deserialize(getString("displayName", "<red>ERROR"));
        description.add(mm.deserialize(getString("description", "")));
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
                TraitLineEntry traitLineEntry = new TraitLineEntry(traitData, levelRequirement, cost, combatOnly);
                traits.add(traitLineEntry);
            }
            traitMap.put(level, traits);
        }
        MessageUtil.log("Loaded traitline " + id + " for class " + hClass.getName());
    }
}
