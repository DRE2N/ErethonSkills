package de.erethon.hecate.commands;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.command.ECommand;
import de.erethon.hecate.util.ResourcepackHandler;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ResourcepackCommand extends ECommand {

    public ResourcepackCommand() {
        setCommand("resourcepack");
        setAliases("rp");
        setMinArgs(0);
        setMaxArgs(1);
        setPermission("hecate.resourcepack");
        setPlayerCommand(true);
        setConsoleCommand(false);
    }

    @Override
    public void onExecute(String[] args, CommandSender commandSender) {
        Player player = (Player) commandSender;
        if (args.length == 1) {
            new ResourcepackHandler(player, player1 -> {
                MessageUtil.sendMessage(player, "<green>Resource pack sent and applied.");
            });
        }
        if (args.length == 2) {
            Player target = player.getServer().getPlayer(args[1]);
            if (target == null) {
                MessageUtil.sendMessage(player, "<red>Player not found.");
                return;
            }
            MessageUtil.sendMessage(player, "<gray>Sending resource pack to " + target.getName() + "...");
            new ResourcepackHandler(target, player1 -> {
                MessageUtil.sendMessage(player, "<green>Resource pack sent and applied to " + target.getName() + ".");
            });
        }
    }
}
