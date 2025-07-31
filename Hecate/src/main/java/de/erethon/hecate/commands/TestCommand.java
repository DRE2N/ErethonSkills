package de.erethon.hecate.commands;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.command.ECommand;
import de.erethon.hecate.Hecate;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

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
        Hecate plugin = Hecate.getInstance();
        if (args[1].equalsIgnoreCase("close")) {
            BukkitRunnable runnable = new BukkitRunnable() {
                @Override
                public void run() {
                    player.closeInventory();
                }
            };
            runnable.runTaskTimer(plugin, 1, 1);
        }
    }
}
