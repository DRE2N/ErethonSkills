package de.erethon.spellbook;


import org.bukkit.plugin.Plugin;

public class Spellbook {
    private static Spellbook instance;
    private SpellQueue queue;
    private Plugin implementingPlugin;

    public Spellbook(Plugin plugin) {
        instance = this;
        implementingPlugin = plugin;
        queue = new SpellQueue(this);
        queue.runTaskTimer(implementingPlugin, 10, 10);
    }

    public static Spellbook getInstance() {
        return instance;
    }

    public SpellQueue getQueue() {
        return queue;
    }
}
