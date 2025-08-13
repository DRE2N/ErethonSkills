package de.erethon.hecate.classes;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.hecate.Hecate;
import de.erethon.hecate.data.HCharacter;
import de.erethon.hecate.casting.SpecialActionKey;
import de.erethon.hecate.progression.LevelInfo;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookAPI;
import de.erethon.spellbook.api.TraitData;
import de.erethon.spellbook.utils.SpellbookTranslator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Color;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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
    private final Set<SpellData> spells = new HashSet<>();
    public List<SpellData> defaultSpellSlots = new ArrayList<>();
    private final Map<SpecialActionKey, SpellData> specialActionMap = new HashMap<>();
    private final Set<TraitData> innateTraits = new HashSet<>();
    private TextColor energyColor = TextColor.fromHexString("#0xFF00");
    private @Nullable String energySymbol = "\u26A1"; // Symbol for energy in the UI
    private final HashSet<String> armorTags = new HashSet<>();
    private final HashSet<String> accessoryTags = new HashSet<>();
    private final HashSet<String> weaponTags = new HashSet<>();
    private Map<Integer, LevelInfo> levelInfo = new HashMap<>();

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

    public Set<SpellData> getSpells() {
        return spells;
    }

    public SpellData getSpecialAction(SpecialActionKey key) {
        return specialActionMap.get(key);
    }

    public Set<TraitData> getInnateTraits() {
        return innateTraits;
    }

    public TextColor getEnergyColor() {
        return energyColor;
    }

    public String getEnergySymbol() {
        return energySymbol;
    }

    public Set<String> getWeaponTags() {
        return weaponTags;
    }

    public Set<String> getArmorTags() {
        return armorTags;
    }

    public Set<String> getAccessoryTags() {
        return accessoryTags;
    }

    public Map<Integer, LevelInfo> getLevelInfo() {
        return levelInfo;
    }

    public void onSwitchTo(HCharacter character) {
        for (TraitData trait : innateTraits) {
            character.getPlayer().addTrait(trait);
        }
    }

    public void onSwitchFrom(HCharacter character) {
        for (TraitData trait : innateTraits) {
            character.getPlayer().removeTrait(trait);
        }
    }

    @SuppressWarnings("removal")
    @Override
    public void load(File file) throws IOException, InvalidConfigurationException {
        super.load(file);
        final SpellbookTranslator translator = Hecate.getInstance().getTranslator();
        id = file.getName().replace(".yml", "");
        displayName = mm.deserialize(getString("displayName", "<red>ERROR"));
        description.add(mm.deserialize(getString("description", "")));
        inactiveModelData = getInt("inactiveModelData", 0);
        activeModelData = getInt("activeModelData", 0);
        initialLevelRequirement = getInt("initialLevelRequirement", 0);
        energyColor = TextColor.fromCSSHexString(getString("energyColor", "#0xFF00"));
        energySymbol = getString("energySymbol", "\u26A1");
        if (contains("weaponTags")) {
            weaponTags.addAll(getStringList("weaponTags"));
        }
        if (contains("armorTags")) {
            armorTags.addAll(getStringList("armorTags"));
        }
        if (contains("accessoryTags")) {
            accessoryTags.addAll(getStringList("accessoryTags"));
        }
        for (String id : getStringList("spells")) {
            SpellData spell = spellbookAPI.getLibrary().getSpellByID(id);
            if (spell == null) {
                Hecate.log("Unknown spell '" + id + "' found for 'spells' in traitline file " + id);
                continue;
            }
            spells.add(spell);
        }
        for (String id : getStringList("defaultSpellSlots")) {
            SpellData spell = spellbookAPI.getLibrary().getSpellByID(id);
            if (spell == null) {
                Hecate.log("Unknown spell '" + id + "' found for 'defaultSpellSlots' in traitline file " + id);
                continue;
            }
            defaultSpellSlots.add(spell);
        }
        ConfigurationSection specialActionSection = getConfigurationSection("specialActionKeys");
        if (specialActionSection != null) {
            for (String key : specialActionSection.getKeys(false)) {
                ConfigurationSection actionSection = specialActionSection.getConfigurationSection(key);
                if (actionSection == null) {
                    Hecate.log("Invalid special action key '" + key + "' found in class file " + getId());
                    continue;
                }
                SpecialActionKey actionKey = SpecialActionKey.valueOf(key.toUpperCase());
                String spellId = actionSection.getString("spell");
                if (spellId == null) {
                    Hecate.log("Invalid special action key '" + key + "' found in class file " + getId());
                    continue;
                }
                SpellData spellData = Hecate.getInstance().getAPI().getLibrary().getSpellByID(spellId);
                if (spellData == null) {
                    Hecate.log("Unknown spell '" + spellId + "' found under 'spells' in class file " + getId());
                    continue;
                }
                specialActionMap.put(actionKey, spellData);
            }
        }
        for (String id : getStringList("innateTraits")) {
            TraitData trait = spellbookAPI.getLibrary().getTraitByID(id);
            if (trait == null) {
                Hecate.log("Unknown trait '" + id + "' found for 'innateTraits' in traitline file " + getId());
                continue;
            }
            innateTraits.add(trait);
        }
        if (getConfigurationSection("traitLine") != null) {
            for (String key : getConfigurationSection("traitLine").getKeys(false)) {
                int level = Integer.parseInt(key);
                List<TraitLineEntry> traits = new ArrayList<>();
                for (String traitId : getConfigurationSection("traitLine." + level).getKeys(false)) {
                    ConfigurationSection traitSection = getConfigurationSection("traitLine." + level + "." + traitId);
                    TraitData traitData = spellbookAPI.getLibrary().getTraitByID(traitId);
                    if (traitData == null) {
                        Hecate.log("Unknown trait '" + traitId + "' found in traitline file " + id);
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
        }
        if (contains("characterLevels")) {
            for (String outerKey : getConfigurationSection("characterLevels").getKeys(false)) {
                int level = Integer.parseInt(outerKey);
                ConfigurationSection levelSection = getConfigurationSection("characterLevels" + outerKey);
                if (levelSection == null) {
                    continue;
                }
                ConfigurationSection attributeSection = levelSection.getConfigurationSection("attributes");
                if (attributeSection == null) {
                    continue;
                }
                HashMap<Attribute, Double> attributes = new HashMap<>();
                for (String attributeName : attributeSection.getKeys(false)) {
                    try {
                        Attribute attribute = Attribute.valueOf(attributeName.toUpperCase());
                        attributes.put(attribute, attributeSection.getDouble(attributeName));
                        Hecate.log("Set " + attribute.name() + " to " + attributeSection.getDouble(attributeName));
                    } catch (IllegalArgumentException e) {
                        Hecate.log("Unknown attribute '" + attributeName + "' found under 'attributes' in class file " + getId());
                    }
                }
                ConfigurationSection messageSection = levelSection.getConfigurationSection("message");
                if (messageSection == null) {
                    continue;
                }
                for (String key : messageSection.getKeys(false)) {
                    if (key.equals("de")) {
                        translator.registerTranslation("characterlevel." + id + "." + level, messageSection.getString(key), Locale.GERMANY);
                    } else {
                        translator.registerTranslation("characterlevel." + id + "." + level, messageSection.getString(key), Locale.US);
                    }
                }
                LevelInfo info = new LevelInfo(level, "characterlevel." + id + "." + level, attributes);
                levelInfo.put(level, info);
            }
            Hecate.log("Loaded " + levelInfo.size() + " character levels for " + id);
        }
        Hecate.log("Loaded traitline " + id + " from " + file.getName() + " with " + traitMap.size() + " levels and " + traitMap.values().stream().mapToInt(List::size).sum() + " traits.");
    }
}
