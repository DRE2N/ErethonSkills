package de.erethon.hecate.commands;

import de.erethon.bedrock.command.ECommand;
import org.bukkit.command.CommandSender;

public class TestCommand extends ECommand {

    public TestCommand() {
        setCommand("test");
        setAliases("s");
        setMinArgs(0);
        setMaxArgs(3);
        setPlayerCommand(true);
        setConsoleCommand(false);
        setHelp("Help.");
        setPermission("hecate.reload");
    }


    @Override
    public void onExecute(String[] args, CommandSender commandSender) {
    }





}

