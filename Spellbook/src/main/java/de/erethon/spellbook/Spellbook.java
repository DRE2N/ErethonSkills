package de.erethon.spellbook;


import de.slikey.effectlib.EffectManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.File;

public class Spellbook  {
    private static Spellbook instance;
    public static File SPELLS;
    private final SpellQueue queue;
    private final SpellLibrary library;
    private final Plugin implementingPlugin;

    private final EffectManager effectManager;

    public Spellbook(Plugin plugin) {
        instance = this;
        implementingPlugin = plugin;

        SPELLS = new File(Bukkit.getPluginsFolder(), "Spellbook");
        if (!SPELLS.exists()) {
            SPELLS.mkdir();
        }

        queue = new SpellQueue(this);
        implementingPlugin.getServer().getPluginManager().registerEvents(queue, implementingPlugin);

        library = new SpellLibrary(this);
        library.loadSpells(SPELLS);
        effectManager = new EffectManager(implementingPlugin);

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

    public EffectManager getEffectManager() {
        return effectManager;
    }
}
