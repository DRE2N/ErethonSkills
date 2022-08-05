package de.erethon.hecate.commands;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.command.ECommand;
import de.erethon.hecate.Hecate;
import de.erethon.hecate.casting.HPlayer;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellbookSpell;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookAPI;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class SkillCommand extends ECommand {

    private final SpellbookAPI spellbook = Hecate.getInstance().getSpellbook();

    public SkillCommand() {
        setCommand("skill");
        setAliases("s");
        setMinArgs(0);
        setMaxArgs(1);
        setPlayerCommand(true);
        setConsoleCommand(false);
        setHelp("Invalid amount of args.");
        setPermission("hecate.skill");
    }

    @Override
    public void onExecute(String[] args, CommandSender commandSender) {
        HPlayer player = new HPlayer(spellbook, (Player) commandSender);
        SpellData spellData = spellbook.getLibrary().getSpellByID(args[1]);

        if (spellData == null) {
            MessageUtil.sendMessage(commandSender, "Invalid spell.");
            return;
        }
        SpellbookSpell spellbookSpell = spellData.queue(player.getPlayer());
        MessageUtil.log("Spell " + spellbookSpell.getData().getId() + " (" + spellbookSpell.getUuid() + ") queued.");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            List<String> completes = new ArrayList<>();
            for (String spell : spellbook.getLibrary().getLoaded().keySet()) {
                if (spell.toLowerCase().startsWith(args[1].toLowerCase())) {
                    completes.add(spell);
                }
            }
            return completes;
        }
        return null;
    }
}
