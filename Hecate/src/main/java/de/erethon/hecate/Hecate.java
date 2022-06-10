package de.erethon.hecate;

import de.erethon.spellbook.Spellbook;
import org.bukkit.plugin.java.JavaPlugin;

public final class Hecate extends JavaPlugin {

    private Spellbook spellbook;

    @Override
    public void onEnable() {
        spellbook = new Spellbook(this);

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
