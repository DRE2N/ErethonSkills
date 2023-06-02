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
        addCommand(new TestCommand());
        addCommand(new LearnSkillCommand());
        addCommand(new ReloadCommand());
        addCommand(new LearnGUICommand());
        addCommand(new AttributeCommand());
        addCommand(new DebugModeCommand());
        addCommand(new TraitCommand());
        addCommand(new TeamCommand());
        addCommand(new EffectCommand());
        addCommand(new ClassCommand());
    }
}
