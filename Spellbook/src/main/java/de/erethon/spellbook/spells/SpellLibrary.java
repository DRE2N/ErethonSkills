package de.erethon.spellbook.spells;

import de.erethon.spellbook.Spellbook;
import org.bukkit.configuration.InvalidConfigurationException;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class SpellLibrary {

    private Spellbook spellbook;
    private HashMap<String, Spell> loaded = new HashMap<>();

    public SpellLibrary(Spellbook spellbook) {
        this.spellbook = spellbook;
    }

    public Spell getSpellByID(String id) {
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
                Spell spell = new Spell(spellbook, id);
                spell.load(f);
                loaded.put(id, spell);
            } catch (IOException | InvalidConfigurationException e) {
                throw new RuntimeException(e);
            }
        }
        spellbook.getImplementingPlugin().getLogger().info("Loaded " + loaded.size() + " spells.");
    }
}
