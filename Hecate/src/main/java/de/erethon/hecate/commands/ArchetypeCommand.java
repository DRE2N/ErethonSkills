package de.erethon.hecate.commands;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.command.ECommand;
import de.erethon.hecate.Hecate;
import de.erethon.hecate.data.HCharacter;
import de.erethon.hecate.classes.HClass;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ArchetypeCommand extends ECommand {

    private final Hecate plugin = Hecate.getInstance();

    public ArchetypeCommand() {
        setCommand("archetype");
        setAliases("c");
        setMinArgs(1);
        setMaxArgs(4);
        setHelp("<red>Invalid syntax. Use /archetype <archetype/list> [player]");
        setPermission("hecate.class");
        setPlayerCommand(true);
        setConsoleCommand(false);
        setRegisterSeparately(true);
    }
    @Override
    public void onExecute(String[] args, CommandSender commandSender) {
        Player player = (Player) commandSender;
        HCharacter hCharacter = plugin.getDatabaseManager().getCurrentCharacter(player);
        if (args.length < 2) {
            MessageUtil.sendMessage(player, "<red>Invalid syntax. Use /archetype <archetype/list>");
            return;
        }
        if (args[1].equalsIgnoreCase("list")) {
            MessageUtil.sendMessage(player, "<green>Loaded archetypes (" + plugin.getHClasses().size() + "):");
            for (HClass hClass : plugin.getHClasses()) {
                MessageUtil.sendMessage(player, "<green>- <gray>" + hClass.getId());
            }
            return;
        }
        HClass hClass = plugin.getHClass(args[1]);
        if (hClass == null) {
            MessageUtil.sendMessage(player, "<red>Archetype " + args[1] + " not found.");
            return;
        }
        if (args.length < 3) {
            hCharacter.setHClass(hClass);
            MessageUtil.sendMessage(player, "<green>Archetype " + hClass.getDisplayName() + " set.");
            return;
        }
        if (args.length < 4) {
            MessageUtil.sendMessage(player, "<red>Invalid syntax. Use /archetype <archetype/list> [player]");
            return;
        }
        Player targetPlayer = Bukkit.getPlayer(args[2]);
        HCharacter target = plugin.getDatabaseManager().getCurrentCharacter(targetPlayer);
        if (target == null) {
            MessageUtil.sendMessage(player, "<red>Player " + args[2] + " not found.");
            return;
        }
        target.setHClass(hClass);
        MessageUtil.sendMessage(player, "<green>Archetype " + hClass.getDisplayName() + " set for " + target.getPlayer().getName() + ".");
    }
}
