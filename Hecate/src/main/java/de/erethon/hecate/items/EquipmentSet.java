package de.erethon.hecate.items;

import de.erethon.spellbook.api.SpellLibrary;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// Equipment sets give different buffs, depending on the amount of items equipped from the set.
// Config example:
// equipmentSets:
//   mySetID:
//     1:
//       - "Trait1"
//       - "Trait2"
//     2:
//       - "Trait3"
//     3:
//       - "Trait4"
//     4:
//       - "Trait5"
public record EquipmentSet(String tag, Map<Integer, Set<TraitData>> effects) {
    public static EquipmentSet fromConfig(String tag, ConfigurationSection config) {
        SpellLibrary spellLibrary = Bukkit.getServer().getSpellbookAPI().getLibrary();
        Map<Integer, Set<TraitData>> effects = new HashMap<>();
        for (String key : config.getKeys(false)) {
            if (!key.matches("\\d+")) {
                continue;
            }
            int count = Integer.parseInt(key);
            ConfigurationSection section = config.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            Set<TraitData> traits = new HashSet<>();
            for (String traitKey : section.getStringList("traits")) {
                TraitData traitData = spellLibrary.getTraitByID(traitKey);
                if (traitData != null) {
                    traits.add(traitData);
                }
            }
            if (count > 4) {
                Bukkit.getLogger().warning("EquipmentSet '" + tag + "' has more than 4 items for count " + count + ". This is useless.");
                continue;
            }
            effects.put(count, traits);
        }
        return new EquipmentSet(tag, effects);
    }
}