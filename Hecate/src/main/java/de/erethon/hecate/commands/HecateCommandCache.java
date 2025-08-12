package de.erethon.hecate.commands;

import de.erethon.bedrock.command.ECommandCache;
import de.erethon.bedrock.plugin.EPlugin;

import java.util.HashSet;
import java.util.Set;

public class HecateCommandCache extends ECommandCache {
    public static final String LABEL = "hecate";
    private static final Set<String> ALIASES = Set.of("h", "hecate", "hec");

    EPlugin plugin;

    public HecateCommandCache(EPlugin plugin) {
        super(LABEL, plugin, ALIASES, new HashSet<>());
        this.plugin = plugin;
        addCommand(new SkillCommand());
        addCommand(new DisplayCommand());
        addCommand(new LearnSkillCommand());
        addCommand(new ReloadCommand());
        addCommand(new LearnGUICommand());
        addCommand(new AttributeCommand());
        addCommand(new DebugModeCommand());
        addCommand(new TraitCommand());
        addCommand(new TeamCommand());
        addCommand(new EffectCommand());
        addCommand(new ClassCommand());
        addCommand(new CharacterCommand());
        addCommand(new ResourcepackCommand());
        addCommand(new ToggleCommand()); // Temporary
        addCommand(new AdminCommand());
        addCommand(new TestCommand());
        addCommand(new TraitlineCommand());
        addCommand(new XpCommand());
    }
}
