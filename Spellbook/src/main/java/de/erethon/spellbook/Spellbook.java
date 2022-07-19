package de.erethon.spellbook;


import de.erethon.spellbook.spells.SpellLibrary;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.File;

public class Spellbook {
    private static Spellbook instance;
    public static File SPELLS;
    private final SpellQueue queue;
    private final SpellLibrary library;
    private final Plugin implementingPlugin;

    public Spellbook(Plugin plugin) {
        instance = this;
        implementingPlugin = plugin;

        SPELLS = new File(Bukkit.getPluginsFolder(), "Spellbook");
        if (!SPELLS.exists()) {
            SPELLS.mkdir();
        }

        library = new SpellLibrary(this);
        library.loadSpells(SPELLS);

        queue = new SpellQueue(this);
        queue.runTaskTimer(implementingPlugin, 10, 10);
    }

    public static Spellbook getInstance() {
        return instance;
    }

    public SpellLibrary getLibrary() {
        return library;
    }

    public Plugin getImplementingPlugin() {
        return implementingPlugin;
    }

    public SpellQueue getQueue() {
        return queue;
    }
}
