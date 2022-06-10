package de.erethon.hecate.commands;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.command.ECommand;
import de.erethon.hecate.Hecate;
import de.erethon.hecate.casting.PlayerCaster;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.spells.ActiveSpell;
import de.erethon.spellbook.spells.Spell;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SkillCommand extends ECommand {

    private Spellbook spellbook = Hecate.getInstance().getSpellbook();

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
        PlayerCaster caster = new PlayerCaster(spellbook, (Player) commandSender);
        Spell spell = spellbook.getLibrary().getSpellByID(args[0]);

        if (spell == null) {
            MessageUtil.sendMessage(commandSender, "Invalid spell.");
            return;
        }
        ActiveSpell activeSpell = spell.queue(caster);
        MessageUtil.log("Spell " + activeSpell.getSpell().getId() + " (" + activeSpell.getUuid() + ") queued.");
    }
}
