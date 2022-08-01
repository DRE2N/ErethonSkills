package de.erethon.spellbook.spells;

import de.erethon.spellbook.Spellbook;
import org.bukkit.configuration.InvalidConfigurationException;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
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
        for (File f : spellFolder.listFiles()) {
            if (!f.getName().endsWith(".yml")) {
                continue;
            }
            String id = f.getName().replace(".yml", "");
            spellbook.getImplementingPlugin().getLogger().info("Loading spell " + id);
            try {
                spellbook.getImplementingPlugin().getLogger().info(this.getClass().getPackageName() + "." + id);
                Object spellClass = Class.forName(this.getClass().getPackageName() + "." + id).getDeclaredConstructor(Spellbook.class, String.class).newInstance(spellbook, id);
                spellbook.getImplementingPlugin().getLogger().info("Spellbook instance: " + spellbook.getClass().getName());
                SpellData spellData = (SpellData) spellClass;
                spellData.load(f);
                loaded.put(id, spellData);
            } catch (IOException | InvalidConfigurationException | ClassNotFoundException | NoSuchMethodException |
                     InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
        spellbook.getImplementingPlugin().getLogger().info("Loaded " + loaded.size() + " spells.");
    }
}
