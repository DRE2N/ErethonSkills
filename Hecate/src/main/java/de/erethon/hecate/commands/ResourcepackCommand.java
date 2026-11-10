package de.erethon.hecate.commands;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.command.ECommand;
import de.erethon.hecate.Hecate;
import de.erethon.hecate.util.ResourcePackServer;
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
            sendPack(player, player);
        }
        if (args.length == 2) {
            Player target = player.getServer().getPlayer(args[1]);
            if (target == null) {
                MessageUtil.sendMessage(player, "<red>Player not found.");
                return;
            }
            sendPack(player, target);
        }
    }

    private void sendPack(Player sender, Player target) {
        ResourcePackServer server = Hecate.getInstance().getResourcePackServer();
        if (server != null && server.send(target)) {
            MessageUtil.sendMessage(sender, "<green>Resource pack sent to " + target.getName() + ".");
        } else {
            MessageUtil.sendMessage(sender, "<red>Resource pack is not ready yet. Check the Hecate log.");
        }
    }
}
