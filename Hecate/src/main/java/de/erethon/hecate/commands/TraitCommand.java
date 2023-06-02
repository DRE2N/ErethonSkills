package de.erethon.hecate.commands;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.command.ECommand;
import de.erethon.hecate.Hecate;
import de.erethon.hecate.casting.HPlayer;
import de.erethon.hecate.ui.TraitMenu;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

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
        HPlayer hPlayer = Hecate.getInstance().getHPlayerCache().getByPlayer(player);
        if (args[1].equalsIgnoreCase("list")) {
            MessageUtil.sendMessage(player, "<green>Traits:");
            for (SpellTrait trait : player.getActiveTraits()) {
                MessageUtil.sendMessage(player, "<green>- <gray>" + trait.getData().getId());
            }
            return;
        }
        if (args[1].equalsIgnoreCase("menu")) {
            TraitMenu menu = new TraitMenu(hPlayer);
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
            MessageUtil.sendMessage(player, "<green>Trait " + traitData.getId()+ " added.");
            return;
        }
        if (args[1].equalsIgnoreCase("remove")) {
            if (player.hasTrait(traitData)) {
                MessageUtil.sendMessage(player, "<red>You don't have this trait.");
                return;
            }
            player.removeTrait(traitData);
            MessageUtil.sendMessage(player, "<green>Trait " + traitData.getId() + " removed.");
            return;
        }
        MessageUtil.sendMessage(player, "<red>Invalid syntax. Use /trait add|remove|list [<trait>]");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            List<String> completes = new ArrayList<>();
            completes.add("add");
            completes.add("remove");
            completes.add("list");
            completes.add("menu");
            return completes;
        }
        if (args.length == 3 && args[1].equalsIgnoreCase("add")) {
            List<String> completes = new ArrayList<>();
            for (String trait : Bukkit.getServer().getSpellbookAPI().getLibrary().getLoadedTraits().keySet()) {
                if (trait.toLowerCase().startsWith(args[2].toLowerCase())) {
                    completes.add(trait);
                }
            }
            return completes;
        }
        if (args.length == 3 && args[1].equalsIgnoreCase("remove")) {
            List<String> completes = new ArrayList<>();
            for (SpellTrait trait : ((Player) sender).getActiveTraits()) {
                if (trait.getData().getId().startsWith(args[2].toLowerCase())) {
                    completes.add(trait.getData().getId());
                }
            }
            return completes;
        }
        return null;
    }
}
