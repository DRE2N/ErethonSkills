package de.erethon.hecate.commands;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.command.ECommand;
import de.erethon.hecate.Hecate;
import de.erethon.hecate.data.Bank;
import de.erethon.hecate.ui.BankInventory;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Admin command to open and edit player banks
 */
public class BankCommand extends ECommand {

    public BankCommand() {
        setCommand("bank");
        setAliases("playerbank");
        setMinArgs(1);
        setMaxArgs(4);
        setHelp("/bank <player> - Opens the bank of a player\n/bank set <player> <pages> - Sets the number of unlocked pages");
        setPermission("hecate.admin.bank");
        setPlayerCommand(true);
        setConsoleCommand(false);
    }

    @Override
    public void onExecute(String[] args, CommandSender sender) {
        Player admin = (Player) sender;

        if (args[1].equalsIgnoreCase("set")) {
            if (args.length < 3) {
                MessageUtil.sendMessage(sender, "<red>Usage: /bank set <player> <pages>");
                return;
            }
            handleSetPages(sender, args[2], args[3]);
            return;
        }

        handleOpenBank(admin, args[1]);
    }
    
    private void handleSetPages(CommandSender sender, String playerName, String pagesStr) {
        int pages;
        try {
            pages = Integer.parseInt(pagesStr);
        } catch (NumberFormatException e) {
            MessageUtil.sendMessage(sender, "<red>Invalid number: " + pagesStr);
            return;
        }
        
        if (pages < Bank.DEFAULT_PAGES || pages > Bank.MAX_PAGES) {
            MessageUtil.sendMessage(sender, "<red>Pages must be between " + Bank.DEFAULT_PAGES + " and " + Bank.MAX_PAGES);
            return;
        }

        Player targetPlayer = Bukkit.getPlayer(playerName);
        UUID targetPlayerId;
        
        if (targetPlayer != null) {
            targetPlayerId = targetPlayer.getUniqueId();
        } else {
            try {
                targetPlayerId = UUID.fromString(playerName);
            } catch (IllegalArgumentException e) {
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
                
                if (offlinePlayer.hasPlayedBefore() || offlinePlayer.isOnline()) {
                    targetPlayerId = offlinePlayer.getUniqueId();
                } else {
                    MessageUtil.sendMessage(sender, "<red>Player not found: " + playerName);
                    return;
                }
            }
        }

        UUID finalTargetPlayerId = targetPlayerId;
        Hecate.getInstance().getDatabaseManager().loadBankData(targetPlayerId).thenAccept(bank -> {
            if (bank == null) {
                MessageUtil.sendMessage(sender, "<red>Failed to load bank data for player " + playerName);
                return;
            }
            
            bank.setUnlockedPages(pages);
            Hecate.getInstance().getDatabaseManager().saveBankData(
                finalTargetPlayerId, 
                bank.serialize(), 
                bank.getUnlockedPages()
            ).thenAccept(success -> {
                if (success) {
                    MessageUtil.sendMessage(sender, "<green>Set bank pages for " + playerName + " to " + pages);
                } else {
                    MessageUtil.sendMessage(sender, "<red>Failed to save bank data for " + playerName);
                }
            });
        }).exceptionally(ex -> {
            MessageUtil.sendMessage(sender, "<red>Error setting bank pages: " + ex.getMessage());
            ex.printStackTrace();
            return null;
        });
    }

    private void handleOpenBank(Player admin, String playerName) {
        Player targetPlayer = Bukkit.getPlayer(playerName);
        UUID targetPlayerId;

        if (targetPlayer != null) {
            targetPlayerId = targetPlayer.getUniqueId();
        } else {
            try {
                targetPlayerId = UUID.fromString(playerName);
            } catch (IllegalArgumentException e) {
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);

                if (offlinePlayer.hasPlayedBefore() || offlinePlayer.isOnline()) {
                    targetPlayerId = offlinePlayer.getUniqueId();
                } else {
                    MessageUtil.sendMessage(admin, "<red>Player not found: " + playerName);
                    return;
                }
            }
        }
        Hecate.getInstance().getDatabaseManager().loadBankData(targetPlayerId).thenAccept(bank -> {
            if (bank == null) {
                MessageUtil.sendMessage(admin, "<red>Failed to load bank data for player " + playerName);
                return;
            }

            Bukkit.getScheduler().runTask(Hecate.getInstance(), () -> {
                new BankInventory(bank, admin, false);
                MessageUtil.sendMessage(admin, "<green>Opened bank for player " + playerName);
            });
        }).exceptionally(ex -> {
            MessageUtil.sendMessage(admin, "<red>Error loading bank: " + ex.getMessage());
            ex.printStackTrace();
            return null;
        });
    }
}
