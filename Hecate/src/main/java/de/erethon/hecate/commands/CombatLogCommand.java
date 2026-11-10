package de.erethon.hecate.commands;

import de.erethon.bedrock.command.ECommand;
import de.erethon.hecate.Hecate;
import de.erethon.hecate.combatlog.CombatLogManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class CombatLogCommand extends ECommand {

    public CombatLogCommand() {
        setCommand("combatlog");
        setAliases("clog");
        setMinArgs(0);
        setMaxArgs(1);
        setPlayerCommand(true);
        setConsoleCommand(false);
        setHelp("Use /h combatlog [on|off|all|status|flush].");
    }

    @Override
    public void onExecute(String[] args, CommandSender sender) {
        Player player = (Player) sender;
        CombatLogManager manager = Hecate.getInstance().getCombatLogManager();
        if (args.length == 1) {
            CombatLogManager.Mode next = manager.getMode(player) == CombatLogManager.Mode.OFF
                    ? CombatLogManager.Mode.SELF
                    : CombatLogManager.Mode.OFF;
            setMode(player, manager, next);
            return;
        }

        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "on", "self" -> setMode(player, manager, CombatLogManager.Mode.SELF);
            case "off" -> setMode(player, manager, CombatLogManager.Mode.OFF);
            case "all" -> {
                if (!manager.canViewAll(player)) {
                    player.sendMessage(Component.text("You need hecate.combatlog.all to watch all combat.",
                            NamedTextColor.RED));
                    return;
                }
                setMode(player, manager, CombatLogManager.Mode.ALL);
            }
            case "status" -> sendStatus(player, manager);
            case "flush" -> {
                manager.flush(player);
                player.sendMessage(Component.text("Pending combat entries flushed.", NamedTextColor.GRAY));
            }
            default -> player.sendMessage(Component.text("Use /h combatlog [on|off|all|status|flush].",
                    NamedTextColor.RED));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length != 2) {
            return super.onTabComplete(sender, args);
        }
        List<String> options = new ArrayList<>(List.of("on", "off", "status", "flush"));
        if (sender instanceof Player player && Hecate.getInstance().getCombatLogManager().canViewAll(player)) {
            options.add("all");
        }
        String input = args[1].toLowerCase(Locale.ROOT);
        return options.stream().filter(option -> option.startsWith(input)).toList();
    }

    private static void setMode(Player player, CombatLogManager manager, CombatLogManager.Mode mode) {
        manager.setMode(player, mode);
        player.sendMessage(Component.text("Combat log: ", NamedTextColor.GRAY)
                .append(Component.text(mode.name().toLowerCase(Locale.ROOT),
                        mode == CombatLogManager.Mode.OFF ? NamedTextColor.RED : NamedTextColor.GREEN)));
    }

    private static void sendStatus(Player player, CombatLogManager manager) {
        CombatLogManager.Mode mode = manager.getMode(player);
        player.sendMessage(Component.text("Combat log is " + mode.name().toLowerCase(Locale.ROOT) + ".",
                mode == CombatLogManager.Mode.OFF ? NamedTextColor.RED : NamedTextColor.GREEN));
    }
}
