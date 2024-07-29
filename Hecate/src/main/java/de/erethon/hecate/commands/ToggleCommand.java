package de.erethon.hecate.commands;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.command.ECommand;
import de.erethon.hecate.Hecate;
import de.erethon.hecate.casting.HCharacter;
import de.erethon.hecate.casting.HPlayer;
import de.erethon.spellbook.api.SpellbookAPI;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ToggleCommand extends ECommand {

    private final SpellbookAPI spellbook = Hecate.getInstance().getAPI();

    public ToggleCommand() {
        setCommand("toggleAutoJoin");
        setAliases("taj");
        setMinArgs(0);
        setMaxArgs(1);
        setPlayerCommand(true);
        setConsoleCommand(false);
        setPermission("hecate.toggleautojoin");
    }

    @Override
    public void onExecute(String[] args, CommandSender commandSender) {
        HPlayer player = Hecate.getInstance().getHPlayerCache().getByPlayer((Player) commandSender);
        player.setAutoJoinWithLastCharacter(!player.isAutoJoinWithLastCharacter());
        MessageUtil.sendMessage(commandSender, "AutoJoin " + (player.isAutoJoinWithLastCharacter() ? "enabled" : "disabled") + ".");
    }
}
