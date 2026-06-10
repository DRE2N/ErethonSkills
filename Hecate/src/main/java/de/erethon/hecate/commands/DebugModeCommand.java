package de.erethon.hecate.commands;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.command.ECommand;
import de.erethon.hecate.Hecate;
import de.erethon.spellbook.Spellbook;
import org.bukkit.command.CommandSender;

import java.util.List;

public class DebugModeCommand extends ECommand {

    public DebugModeCommand() {
        setCommand("debug");
        setAliases("d");
        setMinArgs(0);
        setMaxArgs(1);
        setPlayerCommand(true);
        setConsoleCommand(true);
        setHelp("Invalid syntax. Use /h debug [missingAttributes].");
        setPermission("hecate.debug");
    }

    @Override
    public void onExecute(String[] args, CommandSender commandSender) {
        if (args.length == 2 && args[1].equalsIgnoreCase("missingAttributes")) {
            printMissingAttributes(commandSender);
            return;
        }
        Hecate.getInstance().getSpellbook().setDebug(!Hecate.getInstance().getSpellbook().isDebug());
        MessageUtil.sendMessage(commandSender, "<green>Debug mode is now " + Hecate.getInstance().getSpellbook().isDebug());
    }

    private void printMissingAttributes(CommandSender sender) {
        List<Spellbook.MissingAttribute> missingAttributes = Hecate.getInstance().getSpellbook().getMissingAttributes().stream().toList();
        if (missingAttributes.isEmpty()) {
            String message = "No missing attributes recorded.";
            MessageUtil.sendMessage(sender, "<gray>" + message);
            Spellbook.log(message);
            return;
        }
        String header = "Missing attributes (" + missingAttributes.size() + "):";
        MessageUtil.sendMessage(sender, "<yellow>" + header);
        Spellbook.log(header);
        for (Spellbook.MissingAttribute missingAttribute : missingAttributes) {
            String line = missingAttribute.contentID() + " | " + missingAttribute.id() + " | " + missingAttribute.attributeName();
            MessageUtil.sendMessage(sender, "<gray>" + line);
            Spellbook.log(line);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2 && "missingAttributes".toLowerCase().startsWith(args[1].toLowerCase())) {
            return List.of("missingAttributes");
        }
        return super.onTabComplete(sender, args);
    }
}
