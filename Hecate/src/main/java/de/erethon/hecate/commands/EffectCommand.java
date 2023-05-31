package de.erethon.hecate.commands;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.command.ECommand;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellEffect;
import de.erethon.spellbook.api.SpellTrait;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class EffectCommand extends ECommand {

        public EffectCommand() {
            setCommand("effect");
            setAliases("e");
            setMinArgs(1);
            setMaxArgs(4);
            setHelp("<red>Invalid syntax. Use /effect add|remove|list [<effect>]");
            setPermission("hecate.effect");
            setPlayerCommand(true);
            setConsoleCommand(false);
        }

        @Override
        public void onExecute(String[] args, CommandSender commandSender) {
            Player player = (Player) commandSender;
            if (args[1].equalsIgnoreCase("list")) {
                MessageUtil.sendMessage(player, "<green>Effects:");
                for (SpellEffect effect : player.getEffects()) {
                    MessageUtil.sendMessage(player, "<green>- <gray>" + effect.data.getName());
                }
                return;
            }
            if (args.length < 3) {
                MessageUtil.sendMessage(player, "<red>Invalid syntax. Use /effect add|remove|list [<effect>]");
                return;
            }
            EffectData effectData = Bukkit.getServer().getSpellbookAPI().getLibrary().getEffectByID(args[2]);
            if (effectData == null) {
                MessageUtil.sendMessage(player, "<red>Effect " + args[2] + " not found.");
                return;
            }
            if (args[1].equalsIgnoreCase("add")) {
                if (args.length < 5) {
                    MessageUtil.sendMessage(player, "<red>Invalid syntax. Use /effect add <effect> <duration> <stacks>");
                    return;
                }
                int duration = Integer.parseInt(args[3]);
                int stacks = Integer.parseInt(args[4]);
                player.addEffect(player, effectData, duration, stacks);
                MessageUtil.sendMessage(player, "<green>Effect " + effectData.getName() + " with duration " + duration + " and stacks " + stacks + " added");
                return;
            }
            if (args[1].equalsIgnoreCase("remove")) {
                if (player.hasEffect(effectData)) {
                    MessageUtil.sendMessage(player, "<red>You don't have this effect.");
                    return;
                }
                player.removeEffect(effectData);
                MessageUtil.sendMessage(player, "<green>Effect " + effectData.getName() + " removed.");
                return;
            }
            MessageUtil.sendMessage(player, "<red>Invalid syntax. Use /effect add|remove|list [<effect>]");
        }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            List<String> completes = new ArrayList<>();
            completes.add("add");
            completes.add("remove");
            completes.add("list");
            return completes;
        }
        if (args.length == 3 && args[1].equalsIgnoreCase("add")) {
            List<String> completes = new ArrayList<>();
            for (String effect : Bukkit.getServer().getSpellbookAPI().getLibrary().getLoadedEffects().keySet()) {
                if (effect.toLowerCase().startsWith(args[2].toLowerCase())) {
                    completes.add(effect);
                }
            }
            return completes;
        }
        if (args.length == 3 && args[1].equalsIgnoreCase("remove")) {
            List<String> completes = new ArrayList<>();
            /*for (SpellEffect effect : ((Player) sender).getEffects()) {
                if (effect.data.getId().startsWith(args[2].toLowerCase())) {
                    completes.add(effect.data.getId());
                }
            }*/ // TODO: Make ID not private, lol
            return completes;
        }
        return null;
    }

}
