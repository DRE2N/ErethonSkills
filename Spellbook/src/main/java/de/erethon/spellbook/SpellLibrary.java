package de.erethon.spellbook;

import de.erethon.bedrock.misc.FileUtil;
import org.bukkit.configuration.InvalidConfigurationException;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class SpellLibrary {

    private final Spellbook spellbook;
    private final HashMap<String, SpellData> loadedSpells = new HashMap<>();
    private final HashMap<String, EffectData> loadedEffects = new HashMap<>();

    public SpellLibrary(Spellbook spellbook) {
        this.spellbook = spellbook;
    }

    public HashMap<String, SpellData> getLoaded() {
        return loadedSpells;
    }

    public SpellData getSpellByID(String id) {
        return loadedSpells.get(id);
    }

    public void loadSpells(File spellFolder) {
        for (File f : FileUtil.getFilesForFolder(spellFolder)) {
            if (!f.getName().endsWith(".yml")) {
                continue;
            }
            String id = f.getName().replace(".yml", "");
            if (f.getName().contains("effect")) {
                spellbook.getImplementingPlugin().getLogger().info("Loading effect " + id);
                EffectData effectData = new EffectData(spellbook, id);
                try {
                    effectData.load(f);
                } catch (IOException | InvalidConfigurationException e) {
                    e.printStackTrace();
                    continue;
                }
                loadedEffects.put(id, effectData);
                continue;
            }
            spellbook.getImplementingPlugin().getLogger().info("Loading spell " + id);
            SpellData spellData = new SpellData(spellbook, id);
            try {
                spellData.load(f);
            } catch (IOException | InvalidConfigurationException e) {
                e.printStackTrace();
                continue;
            }
            loadedSpells.put(id, spellData);
        }
        spellbook.getImplementingPlugin().getLogger().info("Loaded " + loadedSpells.size() + " spells.");
        spellbook.getImplementingPlugin().getLogger().info("Loaded " + loadedEffects.size() + " effects.");
    }
}
