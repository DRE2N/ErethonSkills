package de.erethon.hecate.commands;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.command.ECommand;
import de.erethon.hecate.Hecate;
import de.erethon.spellbook.teams.SpellbookTeam;
import de.erethon.spellbook.teams.TeamManager;
import org.bukkit.Color;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class TeamCommand extends ECommand {

    TeamManager teamManager = Hecate.getInstance().getSpellbook().getTeamManager();

    public TeamCommand() {
        setCommand("team");
        setAliases("t");
        setMinArgs(1);
        setMaxArgs(3);
        setHelp("<red>Invalid syntax. Use /h team create|join|leave|color|name|delete <team> [<player/color/name>]");
        setPermission("hecate.team");
        setPlayerCommand(true);
        setConsoleCommand(false);
    }

    @Override
    public void onExecute(String[] args, CommandSender commandSender) {
        Player player = (Player) commandSender;
        Entity target = player.getTargetEntity(8);
        if (args[1].equalsIgnoreCase("create")) {
            if (args.length < 3) {
                MessageUtil.sendMessage(player, "<red>Invalid syntax. Use /h team create <team>");
                return;
            }
            teamManager.createTeam(args[2], args[2], Color.GRAY);
            MessageUtil.sendMessage(player, "<green>Team " + args[2] + " created.");
            return;
        }
        if (args[1].equalsIgnoreCase("delete")) {
            if (args.length < 3) {
                MessageUtil.sendMessage(player, "<red>Invalid syntax. Use /h team delete <team>");
                return;
            }
            teamManager.removeTeam(args[2]);
            MessageUtil.sendMessage(player, "<green>Team " + args[2] + " deleted.");
            return;
        }
        if (args[1].equalsIgnoreCase("join")) {
            if (args.length < 3) {
                MessageUtil.sendMessage(player, "<red>Invalid syntax. Use /h team join <team>");
                return;
            }
            SpellbookTeam team = teamManager.getTeam(args[2]);
            if (team == null) {
                MessageUtil.sendMessage(player, "<red>Team " + args[2] + " not found.");
                return;
            }
            LivingEntity teamEntity = player;
            if (target != null) {
                if (target instanceof LivingEntity) {
                    teamEntity = (LivingEntity) target;
                }
            }
            teamManager.addEntityToTeam(teamEntity, team);
            MessageUtil.sendMessage(player, "<green>" + teamEntity.getName() + " <green>joined team " + args[2] + ".");
            return;
        }
        if (args[1].equalsIgnoreCase("leave")) {
            LivingEntity teamEntity = player;
            if (target != null) {
                if (target instanceof LivingEntity) {
                    teamEntity = (LivingEntity) target;
                }
            }
            teamManager.removeEntityFromTeam(teamEntity);
            MessageUtil.sendMessage(player, "<green>" + teamEntity.getName() + " <green>left team.");
            return;
        }

    }
}
