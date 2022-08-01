package de.erethon.hecate;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.compatibility.Internals;
import de.erethon.bedrock.plugin.EPlugin;
import de.erethon.bedrock.plugin.EPluginSettings;
import de.erethon.hecate.casting.HPlayerCache;
import de.erethon.hecate.commands.HecateCommandCache;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.spells.SpellData;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;

import java.io.File;
import java.util.Map;

public final class Hecate extends EPlugin {

    private static Hecate instance;

    public static File PLAYERS;

    private Spellbook spellbook;
    private HPlayerCache hPlayerCache;
    private HecateCommandCache commands;

    public Hecate() {
        settings = EPluginSettings.builder()
                .internals(Internals.v1_18_R2)
                .forcePaper(true)
                .economy(true)
                .build();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        instance = this;
        loadCore();
    }

    public void loadCore() {
        spellbook = new Spellbook(this);
        MessageUtil.log("Loading spells...");
        for (String spell : spellbook.getLibrary().getLoaded().keySet()) {
            MessageUtil.log("- " + spell);
        }
        initFolders();
        instantiate();
        registerCommands();
    }

    @Override
    public void onDisable() {
        Bukkit.getScheduler().cancelTasks(this);
        HandlerList.unregisterAll(this);
    }

    public void initFolders() {
        initFolder(getDataFolder());
        initFolder(PLAYERS = new File(getDataFolder(), "players"));
    }

    public void initFolder(File folder) {
        if (!folder.exists()) {
            folder.mkdir();
        }
    }

    public void instantiate() {
        hPlayerCache = new HPlayerCache(this);
    }

    public void registerCommands() {
        setCommandCache(commands = new HecateCommandCache(this));
        commands.register(this);
    }

    /* getter */

    public Spellbook getSpellbook() {
        return spellbook;
    }

    public HPlayerCache getHPlayerCache() {
        return hPlayerCache;
    }

    public HecateCommandCache getCommands() {
        return commands;
    }

    public static Hecate getInstance() {
        return instance;
    }

}
