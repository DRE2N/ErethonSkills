package de.erethon.hecate.commands;

import de.erethon.bedrock.command.ECommand;
import de.erethon.hecate.Hecate;
import de.erethon.hecate.casting.HPlayer;
import de.erethon.hecate.ui.SkillMenu;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LearnGUICommand extends ECommand {

    public LearnGUICommand() {
        setCommand("gui");
        setAliases("g");
        setMinArgs(0);
        setMaxArgs(0);
        setPlayerCommand(true);
        setConsoleCommand(false);
        setPermission("hecate.learngui");
    }

    @Override
    public void onExecute(String[] strings, CommandSender commandSender) {
        HPlayer hPlayer = Hecate.getInstance().getHPlayerCache().getByPlayer((Player) commandSender);
        new SkillMenu(hPlayer);
    }

}
