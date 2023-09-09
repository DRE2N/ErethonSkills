package de.erethon.hecate.commands;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.command.ECommand;
import de.erethon.hecate.Hecate;
import de.erethon.hecate.casting.HCharacter;
import de.erethon.hecate.ui.OverviewMenu;
import de.erethon.hecate.ui.SkillMenu;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LearnGUICommand extends ECommand {

    public LearnGUICommand() {
        setCommand("menu");
        setAliases("gui", "m");
        setMinArgs(0);
        setMaxArgs(0);
        setPlayerCommand(true);
        setConsoleCommand(false);
        setPermission("hecate.menu");
    }

    @Override
    public void onExecute(String[] strings, CommandSender commandSender) {
        HCharacter hCharacter = Hecate.getInstance().getHPlayerCache().getCharacter((Player) commandSender);
        if (hCharacter == null) {
            MessageUtil.sendMessage(commandSender, "<red>Character not found. Please relog.");
            return;
        }
        new OverviewMenu(hCharacter);
    }

}
