package de.erethon.hecate.commands;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.command.ECommand;
import de.erethon.hecate.Hecate;
import de.erethon.hecate.casting.HPlayer;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.spells.ActiveSpell;
import de.erethon.spellbook.spells.SpellData;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SkillCommand extends ECommand {

    private final Spellbook spellbook = Hecate.getInstance().getSpellbook();

    public SkillCommand() {
        setCommand("skill");
        setAliases("s");
        setMinArgs(0);
        setMaxArgs(0);
        setPlayerCommand(true);
        setConsoleCommand(false);
        setHelp("Help.");
        setPermission("hecate.reload");
    }

    @Override
    public void onExecute(String[] args, CommandSender commandSender) {
        HPlayer player = new HPlayer(spellbook, (Player) commandSender);
        SpellData spellData = spellbook.getLibrary().getSpellByID(args[0]);

        if (spellData == null) {
            MessageUtil.sendMessage(commandSender, "Invalid spell.");
            return;
        }
        ActiveSpell activeSpell = spellData.queue(player.getCaster());
        MessageUtil.log("Spell " + activeSpell.getSpell().getId() + " (" + activeSpell.getUuid() + ") queued.");
    }
}
