package de.erethon.hecate.commands;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.command.ECommand;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TraitCommand extends ECommand {

    public TraitCommand() {
        setCommand("trait");
        setAliases("t");
        setMinArgs(1);
        setMaxArgs(2);
        setHelp("<red>Invalid syntax. Use /trait add|remove|list [<trait>]");
        setPermission("hecate.trait");
        setPlayerCommand(true);
        setConsoleCommand(false);
    }

    @Override
    public void onExecute(String[] args, CommandSender commandSender) {
        Player player = (Player) commandSender;
        if (args[1].equalsIgnoreCase("list")) {
            MessageUtil.sendMessage(player, "<green>Traits:");
            for (SpellTrait trait : player.getActiveTraits()) {
                MessageUtil.sendMessage(player, "<green>- <gray>" + trait.getData().getName());
            }
            return;
        }
        if (args.length < 3) {
            MessageUtil.sendMessage(player, "<red>Invalid syntax. Use /trait add|remove|list [<trait>]");
            return;
        }
        TraitData traitData = Bukkit.getServer().getSpellbookAPI().getLibrary().getTraitByID(args[2]);
        if (traitData == null) {
            MessageUtil.sendMessage(player, "<red>Trait " + args[2] + " not found.");
            return;
        }
        if (args[1].equalsIgnoreCase("add")) {
            player.addTrait(traitData);
            MessageUtil.sendMessage(player, "<green>Trait " + traitData.getName() + " added.");
            return;
        }
        if (args[1].equalsIgnoreCase("remove")) {
            if (player.hasTrait(traitData)) {
                MessageUtil.sendMessage(player, "<red>You don't have this trait.");
                return;
            }
            player.removeTrait(traitData);
            MessageUtil.sendMessage(player, "<green>Trait " + traitData.getName() + " removed.");
            return;
        }
        MessageUtil.sendMessage(player, "<red>Invalid syntax. Use /trait add|remove|list [<trait>]");
    }


}
