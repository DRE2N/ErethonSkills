package de.erethon.hecate.commands;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.command.ECommand;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AttributeCommand extends ECommand {

    public AttributeCommand() {
        setCommand("attribute");
        setAliases("a");
        setMinArgs(0);
        setMaxArgs(2);
        setPlayerCommand(true);
        setConsoleCommand(false);
        setHelp("Help.");
        setPermission("hecate.attribute");
    }

    @Override
    public void onExecute(String[] args, CommandSender commandSender) {
        Player player = (Player) commandSender;
        Attribute attribute;
        try {
            attribute = Attribute.valueOf(args[1].toUpperCase());
        } catch (IllegalArgumentException e) {
            MessageUtil.sendMessage(commandSender, "Attribute '" + args[1] + "' not found");
            MessageUtil.sendMessage(commandSender, "Available attributes: " + Arrays.toString(Attribute.values()));
            return;
        }
        player.getAttribute(attribute).setBaseValue(Double.parseDouble(args[2]));
        MessageUtil.sendMessage(commandSender, "Set " + attribute.name() + " to " + args[2]);
    }
}
