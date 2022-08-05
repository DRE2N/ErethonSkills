package de.erethon.spellbook;

import de.slikey.effectlib.EffectManager;
import org.bukkit.plugin.Plugin;

public class Spellbook {

    private static Spellbook instance;
    private final Plugin implementer;
    private final EffectManager effectManager;

    public Spellbook(Plugin implementer) {
        instance = this;
        this.implementer = implementer;
        effectManager = new EffectManager(implementer);
    }

    public Plugin getImplementer() {
        return implementer;
    }

    public static Spellbook getInstance() {
        return instance;
    }

    public EffectManager getEffectManager() {
        return effectManager;
    }
}
