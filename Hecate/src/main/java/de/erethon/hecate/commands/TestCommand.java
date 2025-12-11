package de.erethon.hecate.commands;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.command.ECommand;
import org.bukkit.Particle;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TestCommand extends ECommand {

    public TestCommand() {
        setCommand("test");
        setAliases("tst");
        setMinArgs(0);
        setMaxArgs(999);
        setPlayerCommand(true);
        setConsoleCommand(false);
        setHelp("This is a test command.");
        setPermission("hecate.test");
    }

    @Override
    public void onExecute(String[] args, CommandSender commandSender) {
        Player player = (Player) commandSender;
        player.getInventory().getItem(0).setAmount(6969);
    }
}
