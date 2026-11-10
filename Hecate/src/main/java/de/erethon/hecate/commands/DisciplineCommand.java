package de.erethon.hecate.commands;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.command.ECommand;
import de.erethon.hecate.Hecate;
import de.erethon.hecate.classes.Traitline;
import de.erethon.hecate.classes.HClass;
import de.erethon.hecate.data.HCharacter;
import de.erethon.hecate.data.HPlayer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class DisciplineCommand extends ECommand {

    private final Hecate plugin = Hecate.getInstance();

    public DisciplineCommand() {
        setCommand("discipline");
        setAliases("tl");
        setMinArgs(0);
        setMaxArgs(3);
        setHelp("<red>Use /discipline to browse, or /discipline <discipline> [<player>] [force]");
        setPlayerCommand(true);
        setConsoleCommand(false);
        setRegisterSeparately(true);
    }

    @Override
    public void onExecute(String[] args, CommandSender commandSender) {
        Player player = (Player) commandSender;
        HPlayer hPlayer;

        if (args.length < 2) {
            plugin.getDisciplineDialog().open(player);
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

        if (force && !player.isOp() && !player.hasPermission("hecate.discipline.force")) {
            MessageUtil.sendMessage(player, "<red>You cannot force a discipline change.");
            return;
        }
        if (targetPlayer != player && !player.hasPermission("hecate.discipline.others")) {
            MessageUtil.sendMessage(player, "<red>You cannot change another player's discipline.");
            return;
        }

        hPlayer = plugin.getDatabaseManager().getHPlayer(targetPlayer);
        if (hPlayer == null) {
            MessageUtil.sendMessage(player, "<red>Player data not found.");
            return;
        }

        Traitline traitlineData = plugin.getTraitline(traitline);
        if (traitlineData == null) {
            MessageUtil.sendMessage(player, "<red>Discipline " + traitline + " not found.");
            return;
        }

        if (hPlayer.getSelectedCharacter() == null) {
            MessageUtil.sendMessage(player, "<red>" + (targetPlayer == player ? "You" : "The player") + " somehow have no character selected. This shouldn't happen.");
            return;
        }

        HCharacter character = hPlayer.getSelectedCharacter();
        HClass disciplineClass = plugin.getHClasses().stream()
                .filter(candidate -> candidate.getTraitlines().contains(traitlineData))
                .findFirst().orElse(null);
        if (disciplineClass == null) {
            MessageUtil.sendMessage(player, "<red>Discipline " + traitlineData.getId() + " has no class.");
            return;
        }

        if (!force) {
            if (character.getHClass() != disciplineClass) {
                MessageUtil.sendMessage(player, "<red>This discipline belongs to another archetype.");
                return;
            }
            int requirement = Math.max(1, traitlineData.getInitialLevelRequirement());
            if (character.getLevel() < requirement) {
                MessageUtil.sendMessage(player, "<red>This discipline unlocks at level " + requirement + ".");
                return;
            }
        }

        Integer[] traits = character.getTraitline() == traitlineData
                ? character.getSelectedTraits() : new Integer[]{-1, -1, -1};
        Player target = targetPlayer;
        plugin.getDatabaseManager().updateCharacterDiscipline(character.getCharacterID(),
                disciplineClass.getId(), traitlineData.getId(), traits).whenComplete((saved, error) ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (error != null || !Boolean.TRUE.equals(saved)) {
                        MessageUtil.sendMessage(player, "<red>Could not save the discipline change.");
                        return;
                    }
                    character.setSelectedTraits(traits);
                    character.setHClass(disciplineClass);
                    character.setTraitline(traitlineData);
                    if (hPlayer.getSelectedCharacter() == character && character.getCastingManager() != null) {
                        character.getCastingManager().refreshDiscipline();
                    }
                    if (target == player) {
                        MessageUtil.sendMessage(player, "<green>Discipline " + traitlineData.getId() + " set.");
                    } else {
                        MessageUtil.sendMessage(player, "<green>Discipline " + traitlineData.getId() + " set for " + target.getName() + ".");
                        MessageUtil.sendMessage(target, "<green>Your discipline has been set to " + traitlineData.getId() + ".");
                    }
                }));
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
