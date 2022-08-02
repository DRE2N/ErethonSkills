package de.erethon.hecate.commands;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.command.ECommand;
import de.erethon.hecate.Hecate;
import org.bukkit.command.CommandSender;

public class ReloadCommand extends ECommand {

    public ReloadCommand() {
        setCommand("reload");
        setAliases("r");
        setMinArgs(0);
        setMaxArgs(0);
        setPlayerCommand(true);
        setConsoleCommand(true);
        setHelp("Invalid amount of args.");
        setPermission("hecate.reload");
    }

    @Override
    public void onExecute(String[] strings, CommandSender commandSender) {
        Hecate.getInstance().onDisable();
        Hecate.getInstance().onEnable();
        MessageUtil.sendMessage(commandSender, "<green>Reloaded.");
    }

}

