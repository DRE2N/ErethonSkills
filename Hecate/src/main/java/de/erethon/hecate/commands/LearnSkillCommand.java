package de.erethon.hecate.commands;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.command.ECommand;
import de.erethon.hecate.Hecate;
import de.erethon.hecate.casting.HPlayer;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.PassiveSpell;
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
        setMinArgs(1);
        setMaxArgs(2);
        setPlayerCommand(true);
        setConsoleCommand(false);
        setHelp("Invalid amount of args. /h l <Spell> [<Slot>]");
        setPermission("hecate.learn");
    }

    @Override
    public void onExecute(String[] args, CommandSender sender) {
        Player player = (Player) sender;
        HPlayer hPlayer = Hecate.getInstance().getHPlayerCache().getByPlayer(player);
        SpellData spellData = Hecate.getInstance().getAPI().getLibrary().getSpellByID(args[1]);
        if (spellData == null) {
            MessageUtil.sendMessage(sender, "SpellData '" + args[1] + "' not found");
            return;
        }
        /*if (spellData.getSpellClass().isAssignableFrom(PassiveSpell.class)) {
            player.addPassiveSpell(spellData.getActiveSpell(player));
            MessageUtil.sendMessage(sender, "Learned passive spell " + spellData.getId());
        } else if (args.length != 3) {
            MessageUtil.sendMessage(sender, "Active spells require a slot number");
            return;
        }*/
        hPlayer.learnSpell(spellData, Integer.parseInt(args[2]));
        MessageUtil.sendMessage(sender, "Learned spell " + spellData.getId() + " in slot " + args[2]);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            List<String> completes = new ArrayList<>();
            for (String spell : Hecate.getInstance().getAPI().getLibrary().getLoaded().keySet()) {
                if (spell.toLowerCase().startsWith(args[1].toLowerCase())) {
                    completes.add(spell);
                }
            }
            return completes;
        }
        if (args.length == 3) {
            List<String> completes = new ArrayList<>();
            for (String slot : slots) {
                if (slot.startsWith(args[2])) {
                    completes.add(slot);
                }
            }
            return completes;
        }
        return null;
    }
}
