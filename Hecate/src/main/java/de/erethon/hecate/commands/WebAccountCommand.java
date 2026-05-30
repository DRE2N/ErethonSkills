package de.erethon.hecate.commands;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.command.ECommand;
import de.erethon.hecate.Hecate;
import de.erethon.hecate.web.WebAccountService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.net.InetSocketAddress;

public class WebAccountCommand extends ECommand {

    public WebAccountCommand() {
        setCommand("web");
        setMinArgs(1);
        setMaxArgs(2);
        setPlayerCommand(true);
        setConsoleCommand(false);
        setHelp("/web confirm - Confirms a pending web account setup request");
        setRegisterSeparately(true);
    }

    @Override
    public void onExecute(String[] args, CommandSender commandSender) {
        Player player = (Player) commandSender;
        if (args.length < 2 || !args[1].equalsIgnoreCase("confirm")) {
            MessageUtil.sendMessage(player, "<yellow>Use /web confirm after starting setup in your browser.");
            return;
        }
        WebAccountService service = Hecate.getInstance().getWebAccountService();
        InetSocketAddress address = player.getAddress();
        String ip = address == null ? "" : address.getAddress().getHostAddress();
        WebAccountService.ConfirmResult result = service.confirmSetup(player, ip);
        MessageUtil.sendMessage(player, result.success() ? "<green>" + result.message() : "<red>" + result.message());
    }
}
