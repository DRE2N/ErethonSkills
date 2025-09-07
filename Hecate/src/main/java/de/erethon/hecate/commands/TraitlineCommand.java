package de.erethon.hecate.commands;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.command.ECommand;
import de.erethon.hecate.Hecate;
import de.erethon.hecate.classes.Traitline;
import de.erethon.hecate.data.HPlayer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class TraitlineCommand extends ECommand {

    private final Hecate plugin = Hecate.getInstance();

    public TraitlineCommand() {
        setCommand("traitline");
        setAliases("tl");
        setMinArgs(1);
        setMaxArgs(3);
        setHelp("<red>Invalid syntax. Use /traitline <traitline> [<player>] [force]");
        setPermission("hecate.traitline");
        setPlayerCommand(true);
        setConsoleCommand(false);
    }

    @Override
    public void onExecute(String[] args, CommandSender commandSender) {
        Player player = (Player) commandSender;
        HPlayer hPlayer;

        if (args.length < 2) {
            MessageUtil.sendMessage(player, "<red>Invalid syntax. Use /traitline <traitline> [<player>] [force]");
            return;
        }

        String traitline = args[1];
        boolean force = false;
        Player targetPlayer = player;

        if (args.length > 2) {
            if (args[args.length - 1].equalsIgnoreCase("force")) {
                force = true;
                if (args.length == 4) {
                    targetPlayer = Bukkit.getPlayer(args[2]);
                    if (targetPlayer == null) {
                        MessageUtil.sendMessage(player, "<red>Player not found.");
                        return;
                    }
                }
            } else {
                targetPlayer = Bukkit.getPlayer(args[2]);
                if (targetPlayer == null) {
                    MessageUtil.sendMessage(player, "<red>Player not found.");
                    return;
                }
            }
        }

        hPlayer = plugin.getDatabaseManager().getHPlayer(targetPlayer);
        if (hPlayer == null) {
            MessageUtil.sendMessage(player, "<red>Player data not found.");
            return;
        }

        Traitline traitlineData = plugin.getTraitline(traitline);
        if (traitlineData == null) {
            MessageUtil.sendMessage(player, "<red>Traitline " + traitline + " not found.");
            return;
        }

        if (hPlayer.getSelectedCharacter() == null) {
            MessageUtil.sendMessage(player, "<red>" + (targetPlayer == player ? "You" : "The player") + " somehow have no character selected. This shouldn't happen.");
            return;
        }

        // Check permission unless force is used
        if (!force && !targetPlayer.hasPermission("hecate.traitline." + traitlineData.getId())) {
            MessageUtil.sendMessage(player, "<red>Traitline " + traitlineData.getId() + " is not yet unlocked" + (targetPlayer == player ? "" : " for " + targetPlayer.getName()) + ".");
            return;
        }

        hPlayer.getSelectedCharacter().setTraitline(traitlineData);
        if (targetPlayer == player) {
            MessageUtil.sendMessage(player, "<green>Traitline " + traitlineData.getId() + " set.");
        } else {
            MessageUtil.sendMessage(player, "<green>Traitline " + traitlineData.getId() + " set for " + targetPlayer.getName() + ".");
            MessageUtil.sendMessage(targetPlayer, "<green>Your traitline has been set to " + traitlineData.getId() + ".");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        List<String> completes = new ArrayList<>();
        if (args.length == 2) {
            for (Traitline traitline : plugin.getTraitlines()) {
                if (traitline.getId().toLowerCase().startsWith(args[1].toLowerCase())) {
                    completes.add(traitline.getId());
                }
            }
        } else if (args.length == 3) {
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (onlinePlayer.getName().toLowerCase().startsWith(args[2].toLowerCase())) {
                    completes.add(onlinePlayer.getName());
                }
            }
            if ("force".startsWith(args[2].toLowerCase())) {
                completes.add("force");
            }
        } else if (args.length == 4) {
            if ("force".startsWith(args[3].toLowerCase())) {
                completes.add("force");
            }
        }

        return completes;
    }
}
