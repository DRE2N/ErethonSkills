package de.erethon.hecate.commands;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.command.ECommand;
import de.erethon.hecate.Hecate;
import de.erethon.hecate.casting.HPlayer;
import de.erethon.hecate.ui.CharacterSelection;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

public class CharacterCommand extends ECommand {

    public CharacterCommand() {
        setCommand("character");
        setAliases("char");
        setMinArgs(0);
        setMaxArgs(1);
        setPlayerCommand(true);
        setConsoleCommand(false);
        setHelp("Char select");
    }
    @Override
    public void onExecute(String[] args, CommandSender commandSender) {
        HPlayer hplayer = Hecate.getInstance().getHPlayerCache().getByPlayer((Player) commandSender);
        if (args.length > 1 && args[1].equalsIgnoreCase("create")) {
            int id = hplayer.getCharacters().size();
            CraftPlayer player = (CraftPlayer) commandSender;
            ServerPlayer serverPlayer = player.getHandle();
            serverPlayer.server.getPlayerList().switchProfile(serverPlayer, id);
            hplayer.switchCharacterTo(id);
            MessageUtil.sendMessage(commandSender, "<green>Character created with ID " + id + ".");
            return;
        }
        new CharacterSelection(hplayer);
    }
}
