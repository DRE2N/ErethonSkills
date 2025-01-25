package de.erethon.hecate.classes;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.hecate.Hecate;
import de.erethon.hecate.data.HCharacter;
import de.erethon.hecate.casting.SpecialActionKey;
import de.erethon.spellbook.api.SpellData;
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
import java.util.HashSet;
import java.util.List;
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

    @Override
    public void load(File file) throws IOException, InvalidConfigurationException {
        super.load(file);
        id = file.getName().replace(".yml", "");
        displayName = mm.deserialize(getString("displayName", "<red>ERROR"));
        description.add(mm.deserialize(getString("description", "")));
        inactiveModelData = getInt("inactiveModelData", 0);
        activeModelData = getInt("activeModelData", 0);
        initialLevelRequirement = getInt("initialLevelRequirement", 0);
        for (String id : getStringList("spells")) {
            SpellData spell = spellbookAPI.getLibrary().getSpellByID(id);
            if (spell == null) {
                MessageUtil.log("Unknown spell '" + id + "' found for 'spells' in traitline file " + id);
                continue;
            }
            spells.add(spell);
        }
        for (String id : getStringList("defaultSpellSlots")) {
            SpellData spell = spellbookAPI.getLibrary().getSpellByID(id);
            if (spell == null) {
                MessageUtil.log("Unknown spell '" + id + "' found for 'defaultSpellSlots' in traitline file " + id);
                continue;
            }
            defaultSpellSlots.add(spell);
        }
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
        for (String id : getStringList("innateTraits")) {
            TraitData trait = spellbookAPI.getLibrary().getTraitByID(id);
            if (trait == null) {
                MessageUtil.log("Unknown trait '" + id + "' found for 'innateTraits' in traitline file " + id);
                continue;
            }
            innateTraits.add(trait);
        }
        for (String key : getConfigurationSection("traitLine").getKeys(false)) {
            int level = Integer.parseInt(key);
            List<TraitLineEntry> traits = new ArrayList<>();
            for (String traitId : getConfigurationSection("traitLine." + level).getKeys(false)) {
                ConfigurationSection traitSection = getConfigurationSection("traitLine." + level + "." + traitId);
                TraitData traitData = spellbookAPI.getLibrary().getTraitByID(traitId);
                if (traitData == null) {
                    MessageUtil.log("Unknown trait '" + traitId + "' found in traitline file " + id);
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
