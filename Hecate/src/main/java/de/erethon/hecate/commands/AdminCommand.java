package de.erethon.hecate.commands;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.command.ECommand;
import de.erethon.hecate.Hecate;
import de.erethon.hecate.charselection.CharacterDisplay;
import de.erethon.hecate.charselection.CharacterLobby;
import de.erethon.hecate.charselection.CharacterSelection;
import de.erethon.hecate.data.HPlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AdminCommand extends ECommand {

    private final Hecate plugin = Hecate.getInstance();

    public AdminCommand() {
        setCommand("admin");
        setAliases("aa");
        setMinArgs(0);
        setMaxArgs(6);
        setPlayerCommand(true);
        setConsoleCommand(false);
        setHelp("Help.");
        setPermission("hecate.admin");
    }

    @Override
    public void onExecute(String[] args, CommandSender commandSender) {
        if (args.length == 0) {
            MessageUtil.sendMessage(commandSender, "<red>Invalid amount of args.");
            return;
        }
        Player player = (Player) commandSender;
        if (args[1].equalsIgnoreCase("cs") || args[1].equalsIgnoreCase("charsel")) {
            if (args.length == 2) {
                MessageUtil.sendMessage(commandSender, "<red>Invalid amount of args. Try /h admin cs <select/lobby>");
                return;
            }
            if (args[2].equalsIgnoreCase("sel") || args[2].equalsIgnoreCase("select")) {
                CharacterSelection selection = new CharacterSelection(player, plugin.getLobbyInUse());
                MessageUtil.sendMessage(commandSender, "<green>Character selection opened.");
                return;
            }
            if (args[2].equalsIgnoreCase("lobby") || args[2].equalsIgnoreCase("l")) {
                if (args.length == 3) {
                    MessageUtil.sendMessage(commandSender, "<red>Invalid amount of args. Try /h admin cs lobby <create/addpedestal/removepedestal>");
                    return;
                }
                if (args[3].equalsIgnoreCase("create")) {
                    CharacterLobby lobby = new CharacterLobby("default", player.getLocation());
                    MessageUtil.sendMessage(commandSender, "<green>Character lobby created: " + args[4]);
                }
                if (args[3].equalsIgnoreCase("addpedestal") || args[3].equalsIgnoreCase("addped")) {
                    plugin.getLobbyInUse().addPedestal(player.getLocation());
                    MessageUtil.sendMessage(commandSender, "<green>Added pedestal.");
                }
                if (args[3].equalsIgnoreCase("removepedestal") || args[3].equalsIgnoreCase("rmped")) {
                    plugin.getLobbyInUse().removePedestalCloseTo(player.getLocation());
                    MessageUtil.sendMessage(commandSender, "<green>Removed pedestal.");
                }
            }
        }

    }
}
