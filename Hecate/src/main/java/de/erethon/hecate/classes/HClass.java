package de.erethon.hecate.classes;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.hecate.Hecate;
import de.erethon.hecate.casting.SpecialActionKey;
import de.erethon.spellbook.api.SpellData;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HClass extends YamlConfiguration {

    private final HashMap<Integer, Set<SpellData>> spellLevelMap = new HashMap<>();
    private final List<Traitline> traitlines = new ArrayList<>();

    private Traitline defaultTraitline = null;

    private String id;
    private String displayName;
    private TextColor color;
    private String description;
    private int maxLevel;
    private final HashMap<Integer, HashMap<Attribute, Double>> baseAttributesPerLevel = new HashMap<>();
    private final HashMap<Integer, Double> xpPerLevel = new HashMap<>();

    // Equipment
    private final HashSet<String> armorTags = new HashSet<>();
    private final HashSet<String> accessoryTags = new HashSet<>();
    private final HashSet<String> weaponTags = new HashSet<>();

    public HClass(File file) {
        try {
            load(file);
        } catch (IOException | InvalidConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public Set<String> getArmorTags() {
        return armorTags;
    }

    public Set<String> getAccessoryTags() {
        return accessoryTags;
    }

    public Set<String> getWeaponTags() {
        return weaponTags;
    }

    public Set<SpellData> getSpellsUnlockedAtLevel(int level) {
        return spellLevelMap.get(level);
    }

    public HashMap<Attribute, Double> getAttributesPerLevel(int level) {
        return baseAttributesPerLevel.get(level);
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public List<Traitline> getTraitlines() {
        return traitlines;
    }

    public Traitline getStarterTraitline() {
        if (defaultTraitline == null) {
            Hecate.log("Class " + getId() + " has no default traitline configured");
            return null;
        }
        return defaultTraitline;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public TextColor getColor() {
        return color;
    }

    @SuppressWarnings("removal")
    @Override
    public void load(@NotNull File file) throws IOException, InvalidConfigurationException {
        super.load(file);
        id = file.getName().replace(".yml", "");
        displayName = getString("displayName");
        defaultTraitline = Hecate.getInstance().getTraitline(getString("defaultTraitline"));
        color = TextColor.fromHexString(getString("color", "#ffffff"));
        if (contains("armorTags")) {
            armorTags.addAll(getStringList("armorTags"));
        }
        if (contains("accessoryTags")) {
            accessoryTags.addAll(getStringList("accessoryTags"));
        }
        if (contains("weaponTags")) {
            weaponTags.addAll(getStringList("weaponTags"));
        }
        ConfigurationSection spellLevelSection = getConfigurationSection("spellLevels");
        if (spellLevelSection == null) {
            Hecate.log("Class " + getId()+ " has no spell levels configured!");
        } else {
            for (String key : spellLevelSection.getKeys(false)) {
                ConfigurationSection levelEntry = spellLevelSection.getConfigurationSection(key);
                int level = Integer.parseInt(key);
                if (levelEntry != null) {
                    HashSet<SpellData> spells = new HashSet<>();
                    for (String spellId : levelEntry.getStringList("spells")) {
                        SpellData spellData = Hecate.getInstance().getAPI().getLibrary().getSpellByID(spellId);
                        if (spellData == null) {
                            Hecate.log("Unknown spell '" + spellId + "' found under 'spells' in class file " + getId());
                            continue;
                        }
                        spells.add(spellData);
                        spellLevelMap.put(level, spells);
                    }
                }
            }
        }
        for (String id : getStringList("traitlines")) {
            Traitline traitline = Hecate.getInstance().getTraitline(id);
            if (traitline == null) {
                Hecate.log("Unknown traitline '" + id + "' found under 'traitlines' in class file " + getName());
                continue;
            }
            traitlines.add(traitline);
        }
        ConfigurationSection attributesSection = getConfigurationSection("attributeLevels");
        if (attributesSection == null) {
            Hecate.log("Class " + getId() + " has no attributes configured!");
        } else {
            for (String key : attributesSection.getKeys(false)) {
                ConfigurationSection levelEntry = attributesSection.getConfigurationSection(key);
                int level = Integer.parseInt(key);
                if (levelEntry != null) {
                    HashMap<Attribute, Double> attributes = new HashMap<>();
                    for (String attributeName : levelEntry.getKeys(false)) {
                        try {
                            Attribute attribute = Attribute.valueOf(attributeName.toUpperCase());
                            attributes.put(attribute, levelEntry.getDouble(attributeName));
                            Hecate.log("Set " + attribute.name() + " to " + levelEntry.getDouble(attributeName));
                        } catch (IllegalArgumentException e) {
                            Hecate.log("Unknown attribute '" + attributeName + "' found under 'attributes' in class file " + getId());
                        }
                    }
                    baseAttributesPerLevel.put(level, attributes);
                }
            }
        }
        ConfigurationSection levelSection = getConfigurationSection("xpLevels");
        if (levelSection == null) {
            Hecate.log("Class " + getId() + " has no levels configured!");
        } else {
            for (String key : levelSection.getKeys(false)) {
                ConfigurationSection levelEntry = levelSection.getConfigurationSection(key);
                int level = Integer.parseInt(key);
                if (levelEntry != null) {
                    xpPerLevel.put(level, levelEntry.getDouble("xp"));
                }
            }
        }
    }

    public double getXpForLevel(int level) {
        return xpPerLevel.get(level);
    }
}
