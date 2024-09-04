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

    private final Map<SpecialActionKey, SpellData> specialActionMap = new HashMap<>();

    public HClass(File file) {
        try {
            load(file);
        } catch (IOException | InvalidConfigurationException e) {
            throw new RuntimeException(e);
        }
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

    public Traitline getDefaultTraitline() {
        return defaultTraitline;
    }

    public String getId() {
        return id;
    }

    public SpellData getSpecialAction(SpecialActionKey key) {
        return specialActionMap.get(key);
    }

    public String getDisplayName() {
        return displayName;
    }

    public TextColor getColor() {
        return color;
    }

    @Override
    public void load(@NotNull File file) throws IOException, InvalidConfigurationException {
        super.load(file);
        id = file.getName().replace(".yml", "");
        displayName = getString("displayName");
        defaultTraitline = Hecate.getInstance().getTraitline(getString("defaultTraitline"));
        color = TextColor.fromHexString(getString("color", "#ffffff"));
        ConfigurationSection specialActionSection = getConfigurationSection("specialActionKeys");
        if (specialActionSection != null) {
            for (String key : specialActionSection.getKeys(false)) {
                ConfigurationSection actionSection = specialActionSection.getConfigurationSection(key);
                if (actionSection == null) {
                    MessageUtil.log("Invalid special action key '" + key + "' found in class file " + getId());
                    continue;
                }
                SpecialActionKey actionKey = SpecialActionKey.valueOf(key.toUpperCase());
                String spellId = actionSection.getString("spell");
                if (spellId == null) {
                    MessageUtil.log("Invalid special action key '" + key + "' found in class file " + getId());
                    continue;
                }
                SpellData spellData = Hecate.getInstance().getAPI().getLibrary().getSpellByID(spellId);
                if (spellData == null) {
                    MessageUtil.log("Unknown spell '" + spellId + "' found under 'spells' in class file " + getId());
                    continue;
                }
                specialActionMap.put(actionKey, spellData);
            }
        }
        ConfigurationSection spellLevelSection = getConfigurationSection("spellLevels");
        if (spellLevelSection == null) {
            MessageUtil.log("Class " + getId()+ " has no spell levels configured!");
        } else {
            for (String key : spellLevelSection.getKeys(false)) {
                ConfigurationSection levelEntry = spellLevelSection.getConfigurationSection(key);
                int level = Integer.parseInt(key);
                if (levelEntry != null) {
                    HashSet<SpellData> spells = new HashSet<>();
                    for (String spellId : levelEntry.getStringList("spells")) {
                        SpellData spellData = Hecate.getInstance().getAPI().getLibrary().getSpellByID(spellId);
                        if (spellData == null) {
                            MessageUtil.log("Unknown spell '" + spellId + "' found under 'spells' in class file " + getId());
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
                MessageUtil.log("Unknown traitline '" + id + "' found under 'traitlines' in class file " + getName());
                continue;
            }
            traitlines.add(traitline);
        }
        ConfigurationSection attributesSection = getConfigurationSection("attributeLevels");
        if (attributesSection == null) {
            MessageUtil.log("Class " + getId() + " has no attributes configured!");
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
                            MessageUtil.log("Set " + attribute.name() + " to " + levelEntry.getDouble(attributeName));
                        } catch (IllegalArgumentException e) {
                            MessageUtil.log("Unknown attribute '" + attributeName + "' found under 'attributes' in class file " + getId());
                        }
                    }
                    baseAttributesPerLevel.put(level, attributes);
                }
            }
        }
        ConfigurationSection levelSection = getConfigurationSection("xpLevels");
        if (levelSection == null) {
            MessageUtil.log("Class " + getId() + " has no levels configured!");
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
