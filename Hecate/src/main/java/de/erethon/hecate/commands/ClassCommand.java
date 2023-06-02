package de.erethon.hecate.commands;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.command.ECommand;
import de.erethon.hecate.Hecate;
import de.erethon.hecate.casting.HPlayer;
import de.erethon.hecate.classes.HClass;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ClassCommand extends ECommand {

    private final Hecate plugin = Hecate.getInstance();

    public ClassCommand() {
        setCommand("class");
        setAliases("c");
        setMinArgs(1);
        setMaxArgs(4);
        setHelp("<red>Invalid syntax. Use /h class <class/list> [player]");
        setPermission("hecate.class");
        setPlayerCommand(true);
        setConsoleCommand(false);
    }
    @Override
    public void onExecute(String[] args, CommandSender commandSender) {
        Player player = (Player) commandSender;
        HPlayer hPlayer = plugin.getHPlayerCache().getByPlayer(player);
        if (args.length < 2) {
            MessageUtil.sendMessage(player, "<red>Invalid syntax. Use /h class <class/list>");
            return;
        }
        if (args[1].equalsIgnoreCase("list")) {
            MessageUtil.sendMessage(player, "<green>Loaded classes (" + plugin.getHClasses().size() + "):");
            for (HClass hClass : plugin.getHClasses()) {
                MessageUtil.sendMessage(player, "<green>- <gray>" + hClass.getId());
            }
            return;
        }
        HClass hClass = plugin.getHClass(args[1]);
        if (hClass == null) {
            MessageUtil.sendMessage(player, "<red>Class " + args[1] + " not found.");
            return;
        }
        if (args.length < 3) {
            hPlayer.sethClass(hClass);
            MessageUtil.sendMessage(player, "<green>Class " + hClass.getDisplayName() + " set.");
            return;
        }
        HPlayer target = plugin.getHPlayerCache().getByName(args[2]);
        if (target == null) {
            MessageUtil.sendMessage(player, "<red>Player " + args[2] + " not found.");
            return;
        }
        target.sethClass(hClass);
        MessageUtil.sendMessage(player, "<green>Class " + hClass.getDisplayName() + " set for " + target.getPlayer().getName() + ".");
    }
}
