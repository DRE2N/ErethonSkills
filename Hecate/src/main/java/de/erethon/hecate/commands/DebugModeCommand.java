package de.erethon.hecate.commands;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.command.ECommand;
import de.erethon.hecate.Hecate;
import org.bukkit.command.CommandSender;

public class DebugModeCommand extends ECommand {

    public DebugModeCommand() {
        setCommand("debug");
        setAliases("d");
        setMinArgs(0);
        setMaxArgs(0);
        setPlayerCommand(true);
        setConsoleCommand(true);
        setHelp("Help.");
        setPermission("hecate.debug");
    }

    @Override
    public void onExecute(String[] args, CommandSender commandSender) {
        Hecate.getInstance().getSpellbook().setDebug(!Hecate.getInstance().getSpellbook().isDebug());
        MessageUtil.sendMessage(commandSender, "<green>Debug mode is now " + Hecate.getInstance().getSpellbook().isDebug());
    }
}
