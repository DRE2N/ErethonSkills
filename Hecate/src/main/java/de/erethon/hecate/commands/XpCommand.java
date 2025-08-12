package de.erethon.hecate.commands;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.command.ECommand;
import de.erethon.hecate.Hecate;
import de.erethon.hecate.data.HCharacter;
import de.erethon.hecate.data.HPlayer;
import de.erethon.hecate.data.DatabaseManager;
import de.erethon.hecate.progression.LevelUtil;
import de.erethon.tyche.EconomyService;
import de.erethon.tyche.TychePlugin;
import de.erethon.tyche.models.OwnerType;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;

public class XpCommand extends ECommand {

    private final DatabaseManager databaseManager;

    public XpCommand() {
        this.databaseManager = Hecate.getInstance().getDatabaseManager();
        setCommand("xp");
        setAliases("experience", "exp");
        setMinArgs(0);
        setMaxArgs(4);
        setPlayerCommand(true);
        setConsoleCommand(true);
        setHelp("Manage XP and levels for character, alliance, and world seeker progression");
        setPermission("hecate.xp");
    }

    @Override
    public void onExecute(String[] args, CommandSender sender) {
        if (args.length <= 1) {
            showHelp(sender);
            return;
        }

        String action = args[1].toLowerCase();

        switch (action) {
            case "check":
            case "info":
                handleCheck(args, sender);
                break;
            case "give":
            case "add":
                handleGive(args, sender);
                break;
            case "set":
                handleSet(args, sender);
                break;
            default:
                MessageUtil.sendMessage(sender, "<red>Unknown action. Use check, give, or set.");
                break;
        }
    }

    private void handleCheck(String[] args, CommandSender sender) {
        Player target;
        String type = "all";

        if (args.length >= 3) {
            if (args[2].equalsIgnoreCase("character") || args[2].equalsIgnoreCase("alliance") || args[2].equalsIgnoreCase("worldseeker")) {
                type = args[2].toLowerCase();
                target = (Player) sender;
            } else {
                target = Bukkit.getPlayer(args[2]);
                if (target == null) {
                    MessageUtil.sendMessage(sender, "<red>Player not found: " + args[2]);
                    return;
                }
                if (args.length >= 4) {
                    type = args[3].toLowerCase();
                }
            }
        } else {
            if (!(sender instanceof Player)) {
                MessageUtil.sendMessage(sender, "<red>Console must specify a player name.");
                return;
            }
            target = (Player) sender;
        }

        HPlayer hPlayer = databaseManager.getHPlayer(target);
        if (hPlayer == null) {
            MessageUtil.sendMessage(sender, "<red>Player data not loaded for " + target.getName());
            return;
        }

        if (type.equals("all") || type.equals("character")) {
            showCharacterXp(sender, hPlayer);
        }
        if (type.equals("all") || type.equals("alliance")) {
            showAllianceXp(sender, hPlayer);
        }
        if (type.equals("all") || type.equals("worldseeker")) {
            showWorldSeekerXp(sender, hPlayer);
        }
    }

    private void handleGive(String[] args, CommandSender sender) {
        if (args.length < 5) {
            MessageUtil.sendMessage(sender, "<red>Usage: /h xp give <player> <type> <amount>");
            MessageUtil.sendMessage(sender, "<gray>Types: character, alliance, worldseeker");
            return;
        }

        Player target = Bukkit.getPlayer(args[2]);
        if (target == null) {
            MessageUtil.sendMessage(sender, "<red>Player not found: " + args[2]);
            return;
        }

        String type = args[3].toLowerCase();
        long amount;
        try {
            amount = Long.parseLong(args[4]);
        } catch (NumberFormatException e) {
            MessageUtil.sendMessage(sender, "<red>Invalid amount: " + args[4]);
            return;
        }

        if (amount <= 0) {
            MessageUtil.sendMessage(sender, "<red>Amount must be positive.");
            return;
        }

        HPlayer hPlayer = databaseManager.getHPlayer(target);
        if (hPlayer == null) {
            MessageUtil.sendMessage(sender, "<red>Player data not loaded for " + target.getName());
            return;
        }

        switch (type) {
            case "character":
                HCharacter character = hPlayer.getSelectedCharacter();
                if (character == null) {
                    MessageUtil.sendMessage(sender, "<red>" + target.getName() + " has no active character.");
                    return;
                }
                LevelUtil.giveCharacterXp(character, amount);
                MessageUtil.sendMessage(sender, "<green>Gave " + amount + " character XP to " + target.getName());
                break;
            case "alliance":
                LevelUtil.giveAllianceXp(hPlayer, amount);
                MessageUtil.sendMessage(sender, "<green>Gave " + amount + " alliance XP to " + target.getName());
                break;
            case "worldseeker":
                LevelUtil.giveWorldSeekerXp(hPlayer, amount);
                MessageUtil.sendMessage(sender, "<green>Gave " + amount + " world seeker XP to " + target.getName());
                break;
            default:
                MessageUtil.sendMessage(sender, "<red>Invalid type. Use: character, alliance, worldseeker");
                break;
        }
    }

    private void handleSet(String[] args, CommandSender sender) {
        if (args.length < 5) {
            MessageUtil.sendMessage(sender, "<red>Usage: /h xp set <player> <type> <amount>");
            MessageUtil.sendMessage(sender, "<gray>Types: character, alliance, worldseeker");
            return;
        }

        Player target = Bukkit.getPlayer(args[2]);
        if (target == null) {
            MessageUtil.sendMessage(sender, "<red>Player not found: " + args[2]);
            return;
        }

        String type = args[3].toLowerCase();
        long amount;
        try {
            amount = Long.parseLong(args[4]);
        } catch (NumberFormatException e) {
            MessageUtil.sendMessage(sender, "<red>Invalid amount: " + args[4]);
            return;
        }

        if (amount < 0) {
            MessageUtil.sendMessage(sender, "<red>Amount cannot be negative.");
            return;
        }

        HPlayer hPlayer = databaseManager.getHPlayer(target);
        if (hPlayer == null) {
            MessageUtil.sendMessage(sender, "<red>Player data not loaded for " + target.getName());
            return;
        }

        switch (type) {
            case "character":
                HCharacter character = hPlayer.getSelectedCharacter();
                if (character == null) {
                    MessageUtil.sendMessage(sender, "<red>" + target.getName() + " has no active character.");
                    return;
                }
                setXpBalance(character.getCharacterID(), OwnerType.CHARACTER, "xp_character", amount);
                MessageUtil.sendMessage(sender, "<green>Set character XP for " + target.getName() + " to " + amount);
                break;
            case "alliance":
                setXpBalance(hPlayer.getPlayerId(), OwnerType.PLAYER, "xp_alliance", amount);
                MessageUtil.sendMessage(sender, "<green>Set alliance XP for " + target.getName() + " to " + amount);
                break;
            case "worldseeker":
                setXpBalance(hPlayer.getPlayerId(), OwnerType.PLAYER, "xp_exploration", amount);
                MessageUtil.sendMessage(sender, "<green>Set world seeker XP for " + target.getName() + " to " + amount);
                break;
            default:
                MessageUtil.sendMessage(sender, "<red>Invalid type. Use: character, alliance, worldseeker");
                break;
        }
    }

    private void setXpBalance(Object ownerId, OwnerType ownerType, String currency, long amount) {
        TychePlugin tychePlugin = (TychePlugin) Bukkit.getPluginManager().getPlugin("Tyche");
        if (tychePlugin == null) {
            return;
        }
        EconomyService economyService = tychePlugin.getEco();
        java.util.UUID uuid = (java.util.UUID) ownerId;
        economyService.getBalance(uuid, ownerType, currency).thenAccept(currentBalance -> {
            if (currentBalance > 0) {
                economyService.withdraw(uuid, ownerType, currency, currentBalance, "Hecate XP Command", null);
            }
            if (amount > 0) {
                economyService.deposit(uuid, ownerType, currency, amount, "Hecate XP Command", null);
            }
        });
    }

    private void showCharacterXp(CommandSender sender, HPlayer hPlayer) {
        TychePlugin tychePlugin = (TychePlugin) Bukkit.getPluginManager().getPlugin("Tyche");
        if (tychePlugin == null) {
            MessageUtil.sendMessage(sender, "<red>Tyche plugin not found. XP commands require Tyche to be installed.");
            return;
        }
        EconomyService economyService = tychePlugin.getEco();
        HCharacter character = hPlayer.getSelectedCharacter();
        if (character == null) {
            MessageUtil.sendMessage(sender, "<yellow>No active character.");
            return;
        }

        CompletableFuture<Long> xpFuture = economyService.getBalance(character.getCharacterID(), OwnerType.CHARACTER, "xp_character");
        CompletableFuture<Integer> levelFuture = LevelUtil.getCharacterLevel(character);

        CompletableFuture.allOf(xpFuture, levelFuture).thenRun(() -> {
            try {
                long xp = xpFuture.get();
                int level = levelFuture.get();
                MessageUtil.sendMessage(sender, "<aqua>Character XP: <white>" + xp + " <gray>(Level " + level + ")");
            } catch (Exception e) {
                MessageUtil.sendMessage(sender, "<red>Error retrieving character XP data.");
            }
        });
    }

    private void showAllianceXp(CommandSender sender, HPlayer hPlayer) {
        TychePlugin tychePlugin = (TychePlugin) Bukkit.getPluginManager().getPlugin("Tyche");
        if (tychePlugin == null) {
            MessageUtil.sendMessage(sender, "<red>Tyche plugin not found. XP commands require Tyche to be installed.");
            return;
        }
        EconomyService economyService = tychePlugin.getEco();
        CompletableFuture<Long> xpFuture = economyService.getBalance(hPlayer.getPlayerId(), OwnerType.PLAYER, "xp_alliance");
        CompletableFuture<Integer> levelFuture = LevelUtil.getAllianceLevel(hPlayer);

        CompletableFuture.allOf(xpFuture, levelFuture).thenRun(() -> {
            try {
                long xp = xpFuture.get();
                int level = levelFuture.get();
                MessageUtil.sendMessage(sender, "<gold>Alliance XP: <white>" + xp + " <gray>(Level " + level + ")");
            } catch (Exception e) {
                MessageUtil.sendMessage(sender, "<red>Error retrieving alliance XP data.");
            }
        });
    }

    private void showWorldSeekerXp(CommandSender sender, HPlayer hPlayer) {
        TychePlugin tychePlugin = (TychePlugin) Bukkit.getPluginManager().getPlugin("Tyche");
        if (tychePlugin == null) {
            MessageUtil.sendMessage(sender, "<red>Tyche plugin not found. XP commands require Tyche to be installed.");
            return;
        }
        EconomyService economyService = tychePlugin.getEco();
        CompletableFuture<Long> xpFuture = economyService.getBalance(hPlayer.getPlayerId(), OwnerType.PLAYER, "xp_exploration");
        CompletableFuture<Integer> levelFuture = LevelUtil.getWorldSeekerLevel(hPlayer);

        CompletableFuture.allOf(xpFuture, levelFuture).thenRun(() -> {
            try {
                long xp = xpFuture.get();
                int level = levelFuture.get();
                MessageUtil.sendMessage(sender, "<green>World Seeker XP: <white>" + xp + " <gray>(Level " + level + ")");
            } catch (Exception e) {
                MessageUtil.sendMessage(sender, "<red>Error retrieving world seeker XP data.");
            }
        });
    }

    private void showHelp(CommandSender sender) {
        MessageUtil.sendMessage(sender, "<gold>XP Command Help:");
        MessageUtil.sendMessage(sender, "<yellow>/h xp check [player] [type] <gray>- Check XP and levels");
        MessageUtil.sendMessage(sender, "<yellow>/h xp give <player> <type> <amount> <gray>- Give XP");
        MessageUtil.sendMessage(sender, "<yellow>/h xp set <player> <type> <amount> <gray>- Set XP amount");
        MessageUtil.sendMessage(sender, "<gray>Types: character, alliance, worldseeker (or 'all' for check)");
    }
}
