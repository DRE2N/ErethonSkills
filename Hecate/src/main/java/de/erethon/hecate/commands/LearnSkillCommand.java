package de.erethon.hecate.commands;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.command.ECommand;
import de.erethon.hecate.Hecate;
import de.erethon.hecate.casting.HPlayer;
import de.erethon.spellbook.SpellData;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LearnSkillCommand extends ECommand {

    public LearnSkillCommand() {
        setCommand("learn");
        setAliases("l");
        setMinArgs(2);
        setMaxArgs(2);
        setPlayerCommand(true);
        setConsoleCommand(false);
        setHelp("Invalid amount of args. /h l <Spell> <Slot>");
        setPermission("hecate.learn");
    }

    @Override
    public void onExecute(String[] strings, CommandSender commandSender) {
        HPlayer hPlayer = Hecate.getInstance().getHPlayerCache().getByPlayer((Player) commandSender);
        SpellData spellData = Hecate.getInstance().getSpellbook().getLibrary().getSpellByID(strings[1]);
        hPlayer.learnSpell(spellData, Integer.parseInt(strings[2]));
        MessageUtil.sendMessage(commandSender, "Learned spell " + spellData.getId() + " in slot " + strings[2]);
    }

}
