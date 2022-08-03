package de.erethon.spellbook;

import de.erethon.bedrock.misc.FileUtil;
import org.bukkit.configuration.InvalidConfigurationException;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class SpellLibrary {

    private Spellbook spellbook;
    private HashMap<String, SpellData> loaded = new HashMap<>();

    public SpellLibrary(Spellbook spellbook) {
        this.spellbook = spellbook;
    }

    public HashMap<String, SpellData> getLoaded() {
        return loaded;
    }

    public SpellData getSpellByID(String id) {
        return loaded.get(id);
    }

    public void loadSpells(File spellFolder) {
        for (File f : FileUtil.getFilesForFolder(spellFolder)) {
            if (!f.getName().endsWith(".yml")) {
                continue;
            }
            String id = f.getName().replace(".yml", "");
            spellbook.getImplementingPlugin().getLogger().info("Loading spell " + id);
            SpellData spellData = new SpellData(spellbook, id);
            try {
                spellData.load(f);
            } catch (IOException | InvalidConfigurationException e) {
                throw new RuntimeException(e);
            }
            loaded.put(id, spellData);
        }
        spellbook.getImplementingPlugin().getLogger().info("Loaded " + loaded.size() + " spells.");
    }
}
