package de.erethon.hecate.classes;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.hecate.Hecate;
import de.erethon.spellbook.api.SpellData;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class HClass extends YamlConfiguration {

    private final HashMap<Integer, Set<SpellData>> spellLevelMap = new HashMap<>();
    private String displayName;
    private String description;
    private int maxLevel;
    private HashMap<Integer, HashMap<Attribute, Double>> baseAttributesPerLevel = new HashMap<>();
    private HashMap<Integer, Double> xpPerLevel = new HashMap<>();

    public Set<SpellData> getSpellsUnlockedAtLevel(int level) {
        return spellLevelMap.get(level);
    }

    public HashMap<Attribute, Double> getAttributesPerLevel(int level) {
        return baseAttributesPerLevel.get(level);
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    @Override
    public void load(@NotNull File file) throws IOException, InvalidConfigurationException {
        super.load(file);
        ConfigurationSection spellLevelSection = getConfigurationSection("spellLevels");
        if (spellLevelSection == null) {
            MessageUtil.log("Class " + getName() + " has no spell levels configured!");
            return;
        }
        for (String key : spellLevelSection.getKeys(false)) {
            ConfigurationSection levelEntry = spellLevelSection.getConfigurationSection(key);
            int level = Integer.parseInt(key);
            if (levelEntry != null) {
                HashSet<SpellData> spells = new HashSet<>();
                for (String spellId : levelEntry.getStringList("spells")) {
                    SpellData spellData = Hecate.getInstance().getAPI().getLibrary().getSpellByID(spellId);
                    if (spellData == null) {
                        MessageUtil.log("Unknown spell '" + spellId + "' found under 'spells' in class file " + getName());
                        continue;
                    }
                    spells.add(spellData);
                    spellLevelMap.put(level, spells);
                }
            }
        }
        ConfigurationSection attributesSection = getConfigurationSection("attributeLevels");
        if (attributesSection == null) {
            MessageUtil.log("Class " + getName() + " has no attributes configured!");
            return;
        }
        for (String key : attributesSection.getKeys(false)) {
            ConfigurationSection levelEntry = attributesSection.getConfigurationSection(key);
            int level = Integer.parseInt(key);
            if (levelEntry != null) {
                HashMap<Attribute, Double> attributes = new HashMap<>();
                for (String attributeName : levelEntry.getStringList("attributes")) {
                    try {
                        Attribute attribute = Attribute.valueOf(attributeName);
                        attributes.put(attribute, levelEntry.getDouble(attributeName));
                    } catch (IllegalArgumentException e) {
                        MessageUtil.log("Unknown attribute '" + attributeName + "' found under 'attributes' in class file " + getName());
                    }
                }
                baseAttributesPerLevel.put(level, attributes);
            }
        }
        ConfigurationSection levelSection = getConfigurationSection("xpLevels");
        if (levelSection == null) {
            MessageUtil.log("Class " + getName() + " has no levels configured!");
            return;
        }
        for (String key : levelSection.getKeys(false)) {
            ConfigurationSection levelEntry = levelSection.getConfigurationSection(key);
            int level = Integer.parseInt(key);
            if (levelEntry != null) {
                xpPerLevel.put(level, levelEntry.getDouble("xp"));
            }
        }
    }

    public double getXpForLevel(int level) {
        return xpPerLevel.get(level);
    }
}
