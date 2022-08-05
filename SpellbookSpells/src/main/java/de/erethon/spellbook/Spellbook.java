package de.erethon.spellbook;

import de.erethon.spellbook.api.SpellbookAPI;
import de.slikey.effectlib.EffectManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class Spellbook {

    private static Spellbook instance;
    private final SpellbookAPI api;
    private final Plugin implementer;
    private final EffectManager effectManager;

    public Spellbook(Plugin implementer) {
        instance = this;
        this.implementer = implementer;
        this.api = Bukkit.getServer().getSpellbookAPI();
        effectManager = new EffectManager(implementer);
    }

    public Plugin getImplementer() {
        return implementer;
    }

    public SpellbookAPI getAPI() {
        return api;
    }

    public static Spellbook getInstance() {
        return instance;
    }

    public EffectManager getEffectManager() {
        return effectManager;
    }
}
