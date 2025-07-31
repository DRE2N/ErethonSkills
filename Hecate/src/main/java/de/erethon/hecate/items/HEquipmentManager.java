package de.erethon.hecate.items;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.hecate.Hecate;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;

public class HEquipmentManager {

    private YamlConfiguration cfg;

    private final HashMap<String, EquipmentSet> equipmentSets = new HashMap<>();

    public HEquipmentManager(File file) {
        if (!file.exists()) {
            Hecate.log("Equipment configuration file not found: " + file.getAbsolutePath());
            return;
        }
        this.cfg = YamlConfiguration.loadConfiguration(file);
        load();
    }

    public void addEquipmentSet(String tag, EquipmentSet equipmentSet) {
        equipmentSets.put(tag, equipmentSet);
    }
    public EquipmentSet getEquipmentSet(String tag) {
        return equipmentSets.get(tag);
    }

    private void load() {
        ConfigurationSection section = cfg.getConfigurationSection("equipmentSets");
        if (section == null) {
            Hecate.log("No equipment sets found in configuration.");
            return;
        }
        for (String key : section.getKeys(false)) {
            if (cfg.isConfigurationSection(key)) {
                EquipmentSet equipmentSet = EquipmentSet.fromConfig(key, cfg.getConfigurationSection(key));
                equipmentSets.put(key, equipmentSet);
            } else {
                System.err.println("Invalid equipment set configuration: " + key);
            }
        }
    }
}
