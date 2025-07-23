package de.erethon.hecate.commands;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.command.ECommand;
import de.erethon.hecate.Hecate;
import de.erethon.hecate.classes.Traitline;
import de.erethon.hecate.data.HPlayer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TraitlineCommand extends ECommand {

    private final Hecate plugin = Hecate.getInstance();


    public TraitlineCommand() {
        setCommand("traitline");
        setAliases("tl");
        setMinArgs(1);
        setMaxArgs(2);
        setHelp("<red>Invalid syntax. Use /h  <traitline> [<player>]");
        setPermission("hecate.traitline");
        setPlayerCommand(true);
        setConsoleCommand(false);
    }

    @Override
    public void onExecute(String[] args, CommandSender commandSender) {
        Player player = (Player) commandSender;
        HPlayer hPlayer;
        if (args.length < 2) {
            MessageUtil.sendMessage(player, "<red>Invalid syntax. Use /traitline <traitline> [<player>]");
            return;
        }
        String traitline = args[1];
        hPlayer = plugin.getDatabaseManager().getHPlayer(player);
        if (args.length > 2) {
            Player target = Bukkit.getPlayer(args[2]);
            hPlayer = plugin.getDatabaseManager().getHPlayer(target);
            if (hPlayer == null) {
                MessageUtil.sendMessage(player, "<red>Player not found.");
                return;
            }
        }
        Traitline traitlineData = plugin.getTraitline(traitline);
        if (traitlineData == null) {
            MessageUtil.sendMessage(player, "<red>Traitline " + traitline + " not found.");
            return;
        }
        if (hPlayer.getSelectedCharacter() == null) {
            MessageUtil.sendMessage(player, "<red>You somehow have no character selected. This shouldn't happen.");
            return;
        }
        hPlayer.getSelectedCharacter().setTraitline(traitlineData);
        MessageUtil.sendMessage(player, "<green>Traitline " + traitlineData.getId() + " set.");
    }
}
