package de.erethon.hecate.commands;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.command.ECommand;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellEffect;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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

}
