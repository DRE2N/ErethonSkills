package de.erethon.hecate.commands;

import de.erethon.bedrock.command.ECommandCache;
import de.erethon.bedrock.plugin.EPlugin;

public class HecateCommandCache extends ECommandCache {
    public static final String LABEL = "hecate";

    EPlugin plugin;

    public HecateCommandCache(EPlugin plugin) {
        super(LABEL, plugin);
        this.plugin = plugin;
        addCommand(new SkillCommand());
    }
}
