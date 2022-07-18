package de.erethon.hecate;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.compatibility.Internals;
import de.erethon.bedrock.plugin.EPlugin;
import de.erethon.bedrock.plugin.EPluginSettings;
import de.erethon.hecate.commands.HecateCommandCache;
import de.erethon.spellbook.Spellbook;
import org.bukkit.Bukkit;

public final class Hecate extends EPlugin {

    private Spellbook spellbook;
    private static Hecate instance;
    private HecateCommandCache commands;

    public Hecate() {
        settings = EPluginSettings.builder()
                .economy(true)
                .internals(Internals.v1_18_R2)
                .build();
    }
    @Override
    public void onEnable() {
        super.onEnable();
        if (!compat.isPaper()) {
            MessageUtil.log("Please use Paper. https://papermc.io/");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        instance = this;
        commands = new HecateCommandCache(this);
        setCommandCache(commands);
        commands.register(this);
        spellbook = new Spellbook(this);
    }

    @Override
    public void onDisable() {

    }

    public Spellbook getSpellbook() {
        return spellbook;
    }

    public static Hecate getInstance() {
        return instance;
    }

    public HecateCommandCache getCommands() {
        return commands;
    }
}
