package de.erethon.hecate.commands;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.command.ECommand;
import de.erethon.hecate.Hecate;
import de.erethon.hecate.casting.HPlayer;
import de.erethon.spellbook.SpellData;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LearnSkillCommand extends ECommand {

    private final List<String> slots = Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8");

    public LearnSkillCommand() {
        setCommand("learn");
        setAliases("l");
        setMinArgs(2);
        setMaxArgs(3);
        setPlayerCommand(true);
        setConsoleCommand(false);
        setHelp("Invalid amount of args. /h l <Spell> <Slot> [<true/false>]");
        setPermission("hecate.learn");
    }

    @Override
    public void onExecute(String[] strings, CommandSender commandSender) {
        HPlayer hPlayer = Hecate.getInstance().getHPlayerCache().getByPlayer((Player) commandSender);
        SpellData spellData = Hecate.getInstance().getSpellbook().getLibrary().getSpellByID(strings[1]);
        if (strings.length == 3) {
            hPlayer.learnSpell(spellData, Integer.parseInt(strings[2]));
            MessageUtil.sendMessage(commandSender, "Learned spell " + spellData.getId() + " in slot " + strings[2]);
        } else {
            hPlayer.addPassiveSpell(spellData.getActiveSpell(hPlayer));
            MessageUtil.sendMessage(commandSender, "Learned passive spell " + spellData.getId());
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            List<String> completes = new ArrayList<>();
            for (String spell : Hecate.getInstance().getSpellbook().getLibrary().getLoaded().keySet()) {
                if (spell.toLowerCase().startsWith(args[1].toLowerCase())) {
                    completes.add(spell);
                }
            }
            return completes;
        }
        if (args.length == 3) {
            List<String> completes = new ArrayList<>();
            for (String slot : slots) {
                if (slot.startsWith(args[1])) {
                    completes.add(slot);
                }
            }
            return completes;
        }
        return null;
    }
}
