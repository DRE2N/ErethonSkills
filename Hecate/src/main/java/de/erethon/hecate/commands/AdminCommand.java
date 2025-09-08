package de.erethon.hecate.commands;

import de.erethon.bedrock.command.ECommand;
import de.erethon.hecate.Hecate;
import de.erethon.hecate.charselection.CharacterLobby;
import de.erethon.hecate.charselection.CharacterSelection;
import de.erethon.hecate.charselection.ClassSelection;
import de.erethon.hecate.data.HPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class AdminCommand extends ECommand implements TabCompleter {

    private final Hecate plugin = Hecate.getInstance();

    public AdminCommand() {
        setCommand("hecateadmin");
        setAliases("hadmin", "ha");
        setMinArgs(0);
        setMaxArgs(10);
        setPlayerCommand(true);
        setConsoleCommand(false);
        setHelp("Hecate administration commands");
        setPermission("hecate.admin");
    }

    @Override
    public void onExecute(String[] args, CommandSender commandSender) {
        Player player = (Player) commandSender;

        if (args.length == 1) {
            showHelp(player);
            return;
        }

        switch (args[1].toLowerCase()) {
            case "charsel", "characterselection" -> handleCharacterSelection(player, args);
            case "lobby" -> handleLobby(player, args);
            case "classtest" -> handleClassTest(player, args);
            case "characters", "chars" -> handleCharacterManagement(player, args);
            case "reload" -> handleReload(player);
            case "help" -> showHelp(player);
            default -> {
                player.sendMessage(Component.translatable("hecate.commands.admin.unknown_command"));
            }
        }
    }

    private void showHelp(Player player) {
        player.sendMessage(Component.text(">>>", NamedTextColor.GOLD));
        player.sendMessage(Component.translatable("hecate.commands.admin.help_title"));
        player.sendMessage(Component.text(">>>", NamedTextColor.GOLD));
        player.sendMessage(Component.translatable("hecate.commands.admin.help_charsel"));
        player.sendMessage(Component.translatable("hecate.commands.admin.help_charsel_open"));
        player.sendMessage(Component.translatable("hecate.commands.admin.help_charsel_test"));
        player.sendMessage(Component.empty());
        player.sendMessage(Component.translatable("hecate.commands.admin.help_lobby"));
        player.sendMessage(Component.translatable("hecate.commands.admin.help_lobby_create"));
        player.sendMessage(Component.translatable("hecate.commands.admin.help_lobby_addped"));
        player.sendMessage(Component.translatable("hecate.commands.admin.help_lobby_removeped"));
        player.sendMessage(Component.translatable("hecate.commands.admin.help_lobby_info"));
        player.sendMessage(Component.empty());
        player.sendMessage(Component.translatable("hecate.commands.admin.help_system"));
        player.sendMessage(Component.translatable("hecate.commands.admin.help_reload"));
        player.sendMessage(Component.text(">>>", NamedTextColor.GOLD));
    }

    private void handleCharacterSelection(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Component.translatable("hecate.commands.admin.charsel_usage"));
            return;
        }

        switch (args[2].toLowerCase()) {
            case "open" -> {
                if (plugin.getLobbyInUse() == null) {
                    player.sendMessage(Component.translatable("hecate.commands.admin.lobby_no_exists"));
                    return;
                }
                new CharacterSelection(player, plugin.getLobbyInUse());
                player.sendMessage(Component.translatable("hecate.commands.admin.charsel_opened"));
            }
            default -> player.sendMessage(Component.translatable("hecate.commands.admin.charsel_unknown"));
        }
    }

    private void handleLobby(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Component.translatable("hecate.commands.admin.lobby_usage"));
            return;
        }

        switch (args[2].toLowerCase()) {
            case "create" -> {
                String lobbyId = args.length > 3 ? args[3] : "default";
                CharacterLobby lobby = new CharacterLobby(lobbyId, player.getLocation());
                player.sendMessage(Component.translatable("hecate.commands.admin.lobby_created", Component.text(lobbyId)));
                player.sendMessage(Component.translatable("hecate.commands.admin.lobby_created_help"));
            }
            case "addped", "addpedestal" -> {
                if (plugin.getLobbyInUse() == null) {
                    player.sendMessage(Component.translatable("hecate.commands.admin.lobby_no_exists"));
                    return;
                }
                plugin.getLobbyInUse().addPedestal(player.getLocation());
                player.sendMessage(Component.translatable("hecate.commands.admin.lobby_pedestal_added"));
                player.sendMessage(Component.translatable("hecate.commands.admin.lobby_pedestal_count",
                    Component.text(plugin.getLobbyInUse().getPedestalLocations().size())));
            }
            case "removeped", "removepedestal" -> {
                if (plugin.getLobbyInUse() == null) {
                    player.sendMessage(Component.translatable("hecate.commands.admin.lobby_no_exists_remove"));
                    return;
                }
                int beforeCount = plugin.getLobbyInUse().getPedestalLocations().size();
                plugin.getLobbyInUse().removePedestalCloseTo(player.getLocation());
                int afterCount = plugin.getLobbyInUse().getPedestalLocations().size();

                if (beforeCount > afterCount) {
                    player.sendMessage(Component.translatable("hecate.commands.admin.lobby_pedestal_removed"));
                    player.sendMessage(Component.translatable("hecate.commands.admin.lobby_pedestal_count",
                        Component.text(afterCount)));
                } else {
                    player.sendMessage(Component.translatable("hecate.commands.admin.lobby_pedestal_not_found"));
                }
            }
            case "info" -> {
                if (plugin.getLobbyInUse() == null) {
                    player.sendMessage(Component.translatable("hecate.commands.admin.lobby_not_configured"));
                    return;
                }
                CharacterLobby lobby = plugin.getLobbyInUse();
                player.sendMessage(Component.text("═══════════════════════════════════════", NamedTextColor.GOLD));
                player.sendMessage(Component.translatable("hecate.commands.admin.lobby_info_title"));
                player.sendMessage(Component.text("═══════════════════════════════════════", NamedTextColor.GOLD));
                player.sendMessage(Component.translatable("hecate.commands.admin.lobby_info_origin",
                    Component.text(formatLocation(lobby.getOrigin()))));
                player.sendMessage(Component.translatable("hecate.commands.admin.lobby_info_pedestals",
                    Component.text(lobby.getPedestalLocations().size())));

                for (int i = 0; i < lobby.getPedestalLocations().size(); i++) {
                    player.sendMessage(Component.text("  " + (i + 1) + ". " + formatLocation(lobby.getPedestalLocations().get(i)), NamedTextColor.GRAY));
                }
                player.sendMessage(Component.text("═══════════════════════════════════════", NamedTextColor.GOLD));
            }
            default -> player.sendMessage(Component.translatable("hecate.commands.admin.lobby_unknown_command"));
        }
    }

    private void handleClassTest(Player player, String[] args) {
        if (plugin.getLobbyInUse() == null) {
            player.sendMessage(Component.translatable("hecate.commands.admin.lobby_no_exists"));
            return;
        }

        HPlayer hPlayer = plugin.getDatabaseManager().getHPlayer(player);
        if (hPlayer == null) {
            player.sendMessage(Component.translatable("hecate.data.player_not_found"));
            return;
        }

        new ClassSelection(player, plugin.getLobbyInUse());
        player.sendMessage(Component.translatable("hecate.commands.admin.classtest_opened"));
    }

    private void handleReload(Player player) {
        player.sendMessage(Component.translatable("hecate.commands.admin.reloading"));
        // TODO
        player.sendMessage(Component.translatable("hecate.commands.admin.reloaded"));
    }

    private void handleCharacterManagement(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Component.translatable("hecate.commands.admin.characters_usage"));
            return;
        }

        switch (args[2].toLowerCase()) {
            case "deleted" -> {
                if (args.length < 4) {
                    player.sendMessage(Component.translatable("hecate.commands.admin.characters_deleted_usage"));
                    return;
                }

                String targetPlayerName = args[3];
                org.bukkit.OfflinePlayer targetPlayer = plugin.getServer().getOfflinePlayer(targetPlayerName);

                if (!targetPlayer.hasPlayedBefore() && !targetPlayer.isOnline()) {
                    player.sendMessage(Component.translatable("hecate.commands.admin.player_not_found", Component.text(targetPlayerName)));
                    return;
                }

                plugin.getDatabaseManager().getDeletedCharactersForPlayer(targetPlayer.getUniqueId())
                        .thenAccept(deletedCharacters -> {
                            if (deletedCharacters.isEmpty()) {
                                player.sendMessage(Component.translatable("hecate.commands.admin.no_deleted_characters", Component.text(targetPlayerName)));
                                return;
                            }

                            player.sendMessage(Component.text("═══════════════════════════════════════", NamedTextColor.GOLD));
                            player.sendMessage(Component.translatable("hecate.commands.admin.deleted_characters_title", Component.text(targetPlayerName)));
                            player.sendMessage(Component.text("═══════════════════════════════════════", NamedTextColor.GOLD));

                            for (de.erethon.hecate.data.HCharacter character : deletedCharacters) {
                                Component className = character.getHClass() != null ?
                                    character.getHClass().getDisplayName() :
                                    Component.translatable("hecate.misc.no_class", NamedTextColor.GRAY);

                                player.sendMessage(Component.text("• ", NamedTextColor.GRAY)
                                    .append(Component.text("ID: ", NamedTextColor.DARK_GRAY))
                                    .append(Component.text(character.getCharacterID().toString().substring(0, 8) + "...", NamedTextColor.YELLOW))
                                    .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                                    .append(className)
                                    .append(Component.text(" (Level " + character.getLevel() + ")", NamedTextColor.GRAY)));
                            }

                            player.sendMessage(Component.empty());
                            player.sendMessage(Component.translatable("hecate.commands.admin.characters_restore_help"));
                            player.sendMessage(Component.text("═══════════════════════════════════════", NamedTextColor.GOLD));
                        })
                        .exceptionally(ex -> {
                            player.sendMessage(Component.translatable("hecate.data.error_loading", Component.text(ex.getMessage())));
                            ex.printStackTrace();
                            return null;
                        });
            }
            case "restore" -> {
                if (args.length < 4) {
                    player.sendMessage(Component.translatable("hecate.commands.admin.characters_restore_usage"));
                    return;
                }

                String characterIdStr = args[3];
                java.util.UUID characterId;

                try {
                    characterId = java.util.UUID.fromString(characterIdStr);
                } catch (IllegalArgumentException e) {
                    player.sendMessage(Component.translatable("hecate.commands.admin.invalid_character_id"));
                    return;
                }

                plugin.getDatabaseManager().restoreCharacter(characterId)
                        .thenAccept(success -> {
                            if (success) {
                                player.sendMessage(Component.translatable("hecate.commands.admin.character_restored",
                                    Component.text(characterId.toString().substring(0, 8) + "...")));
                            } else {
                                player.sendMessage(Component.translatable("hecate.commands.admin.character_restore_failed",
                                    Component.text(characterId.toString().substring(0, 8) + "...")));
                            }
                        })
                        .exceptionally(ex -> {
                            player.sendMessage(Component.translatable("hecate.data.error_saving", Component.text(ex.getMessage())));
                            ex.printStackTrace();
                            return null;
                        });
            }
            case "all" -> {
                if (args.length < 4) {
                    player.sendMessage(Component.translatable("hecate.commands.admin.characters_all_usage"));
                    return;
                }

                String targetPlayerName = args[3];
                org.bukkit.OfflinePlayer targetPlayer = plugin.getServer().getOfflinePlayer(targetPlayerName);

                if (!targetPlayer.hasPlayedBefore() && !targetPlayer.isOnline()) {
                    player.sendMessage(Component.translatable("hecate.commands.admin.player_not_found", Component.text(targetPlayerName)));
                    return;
                }

                plugin.getDatabaseManager().getAllCharactersForPlayer(targetPlayer.getUniqueId(), true)
                        .thenAccept(allCharacters -> {
                            if (allCharacters.isEmpty()) {
                                player.sendMessage(Component.translatable("hecate.commands.admin.no_characters", Component.text(targetPlayerName)));
                                return;
                            }

                            player.sendMessage(Component.text("═══════════════════════════════════════", NamedTextColor.GOLD));
                            player.sendMessage(Component.translatable("hecate.commands.admin.all_characters_title", Component.text(targetPlayerName)));
                            player.sendMessage(Component.text("═══════════════════════════════════════", NamedTextColor.GOLD));

                            for (de.erethon.hecate.data.HCharacter character : allCharacters) {
                                Component className = character.getHClass() != null ?
                                    character.getHClass().getDisplayName() :
                                    Component.translatable("hecate.misc.no_class", NamedTextColor.GRAY);

                                // Check if character is deleted by looking at the database fields
                                boolean isDeleted = false; // We'll need to add this info to HCharacter or check differently

                                Component statusIcon = isDeleted ?
                                    Component.text("🗑️ ", NamedTextColor.RED) :
                                    Component.text("✅ ", NamedTextColor.GREEN);

                                player.sendMessage(statusIcon
                                    .append(Component.text("ID: ", NamedTextColor.DARK_GRAY))
                                    .append(Component.text(character.getCharacterID().toString().substring(0, 8) + "...", NamedTextColor.YELLOW))
                                    .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                                    .append(className)
                                    .append(Component.text(" (Level " + character.getLevel() + ")", NamedTextColor.GRAY)));
                            }

                            player.sendMessage(Component.text("═══════════════════════════════════════", NamedTextColor.GOLD));
                        })
                        .exceptionally(ex -> {
                            player.sendMessage(Component.translatable("hecate.data.error_loading", Component.text(ex.getMessage())));
                            ex.printStackTrace();
                            return null;
                        });
            }
            default -> player.sendMessage(Component.translatable("hecate.commands.admin.characters_unknown_command"));
        }
    }

    private String formatLocation(org.bukkit.Location loc) {
        if (loc == null) return "null";
        return String.format("%s: %.1f, %.1f, %.1f",
            loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ());
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 2) {
            List<String> subcommands = List.of("charsel", "lobby", "classtest", "reload", "help");
            String input = args[1].toLowerCase();
            for (String cmd : subcommands) {
                if (cmd.startsWith(input)) {
                    completions.add(cmd);
                }
            }
        } else if (args.length == 3) {
            switch (args[1].toLowerCase()) {
                case "charsel", "characterselection" -> {
                    if ("open".startsWith(args[2].toLowerCase())) {
                        completions.add("open");
                    }
                }
                case "lobby" -> {
                    List<String> lobbyCommands = List.of("create", "addped", "removeped", "info");
                    String input = args[2].toLowerCase();
                    for (String cmd : lobbyCommands) {
                        if (cmd.startsWith(input)) {
                            completions.add(cmd);
                        }
                    }
                }
            }
        } else if (args.length == 4 && args[1].equalsIgnoreCase("lobby") && args[2].equalsIgnoreCase("create")) {
            completions.add("default");
            completions.add("lobby1");
            completions.add("spawn");
        }

        return completions;
    }
}
