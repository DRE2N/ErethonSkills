package de.erethon.hecate.commands;

import de.erethon.bedrock.command.ECommand;
import de.erethon.hecate.Hecate;
import de.erethon.hecate.charselection.CharacterSelection;
import de.erethon.hecate.data.HCharacter;
import de.erethon.hecate.data.HPlayer;
import de.erethon.hecate.data.DatabaseManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class CharacterCommand extends ECommand implements TabCompleter {

    private final DatabaseManager databaseManager;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public CharacterCommand() {
        this.databaseManager = Hecate.getInstance().getDatabaseManager();
        setCommand("character");
        setAliases("char", "c");
        setMinArgs(0);
        setMaxArgs(4);
        setPlayerCommand(true);
        setConsoleCommand(false);
        setHelp("Character management - /char [list|select|create|info]");
    }

    public void onExecute(String[] args, CommandSender commandSender) {
        Player player = (Player) commandSender;
        HPlayer hPlayer = databaseManager.getHPlayer(player);

        if (hPlayer == null) {
            player.sendMessage(Component.translatable("hecate.data.not_loaded"));
            return;
        }

        if (args.length == 1 || (args.length == 2 && args[1].equalsIgnoreCase("list"))) {
            showCharacterList(player, hPlayer);
            return;
        }

        switch (args[1].toLowerCase()) {
            case "select", "switch" -> handleCharacterSelect(player, hPlayer, args);
            case "create" -> handleCharacterCreate(player, hPlayer, args);
            case "info" -> handleCharacterInfo(player, hPlayer, args);
            case "selection" -> {
                if (Hecate.getInstance().getLobbyInUse() == null) {
                    player.sendMessage(Component.translatable("hecate.commands.character.no_lobby_configured"));
                    return;
                }
                try {
                    new CharacterSelection(player, Hecate.getInstance().getLobbyInUse());
                } catch (Exception e) {
                    player.sendMessage(Component.translatable("hecate.commands.character.failed_to_open_selection"));
                    e.printStackTrace();
                    return;
                }
                player.sendMessage(Component.translatable("hecate.commands.character.opening_selection"));
            }
            default -> {
                player.sendMessage(Component.translatable("hecate.commands.character.unknown_subcommand"));
                player.sendMessage(Component.translatable("hecate.commands.character.unknown_subcommand_help"));
            }
        }
    }

    private void showCharacterList(Player player, HPlayer hPlayer) {
        List<HCharacter> characters = hPlayer.getCharacters();

        player.sendMessage(Component.text("═══════════════════════════════════════", NamedTextColor.GOLD));
        player.sendMessage(Component.translatable("hecate.commands.character.list_title"));
        player.sendMessage(Component.text("═══════════════════════════════════════", NamedTextColor.GOLD));

        if (characters.isEmpty()) {
            player.sendMessage(Component.translatable("hecate.character.selection.no_characters"));
            player.sendMessage(Component.translatable("hecate.character.selection.no_characters_help"));
        } else {
            for (int i = 0; i < characters.size(); i++) {
                HCharacter character = characters.get(i);
                boolean isSelected = hPlayer.getSelectedCharacter() != null &&
                                   hPlayer.getSelectedCharacter().getCharacterID().equals(character.getCharacterID());

                String prefix = isSelected ? "★ " : (i + 1) + ". ";
                Component className = character.getHClass() != null ? character.getHClass().getDisplayName() : Component.text("Classless", NamedTextColor.GRAY);

                Component message = Component.text(prefix, isSelected ? NamedTextColor.GREEN : NamedTextColor.GRAY)
                    .append(className)
                    .append(Component.text(" - Level " + character.getLevel(), NamedTextColor.GRAY));

                if (isSelected) {
                    message = message.append(Component.translatable("hecate.character.selection.current_indicator"));
                }

                player.sendMessage(message);
                player.sendMessage(Component.text("  Created: " + dateFormat.format(character.getCreatedAt()), NamedTextColor.DARK_GRAY));
            }

            player.sendMessage(Component.empty());
            player.sendMessage(Component.text("Commands:", NamedTextColor.YELLOW));
            player.sendMessage(Component.text("• /char select <number> - Switch to character", NamedTextColor.GRAY));
            player.sendMessage(Component.text("• /char info <number> - View character details", NamedTextColor.GRAY));
            player.sendMessage(Component.text("• /char selection - Open character selection GUI", NamedTextColor.GRAY));
        }
        player.sendMessage(Component.text("═══════════════════════════════════════", NamedTextColor.GOLD));
    }

    private void handleCharacterSelect(Player player, HPlayer hPlayer, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Component.translatable("hecate.commands.character.usage_select"));
            player.sendMessage(Component.translatable("hecate.commands.character.usage_select_help"));
            return;
        }

        int characterIndex;
        try {
            characterIndex = Integer.parseInt(args[2]) - 1;
        } catch (NumberFormatException e) {
            player.sendMessage(Component.translatable("hecate.commands.character.invalid_number"));
            return;
        }

        List<HCharacter> characters = hPlayer.getCharacters();
        if (characterIndex < 0 || characterIndex >= characters.size()) {
            player.sendMessage(Component.translatable("hecate.commands.character.number_out_of_range",
                Component.text(characters.size())));
            return;
        }

        HCharacter targetCharacter = characters.get(characterIndex);

        if (hPlayer.getSelectedCharacter() != null &&
            hPlayer.getSelectedCharacter().getCharacterID().equals(targetCharacter.getCharacterID())) {
            player.sendMessage(Component.translatable("hecate.character.selection.already_selected"));
            return;
        }

        // Visual feedback
        player.showTitle(Title.title(Component.empty(), Component.translatable("hecate.character.selection.switching")));

        // Switch character
        hPlayer.setSelectedCharacter(targetCharacter, false);

        Component className = targetCharacter.getHClass() != null
            ? targetCharacter.getHClass().getDisplayName()
            : Component.text("No Class", NamedTextColor.GRAY);
        player.sendMessage(Component.translatable("hecate.character.selection.switched",
            className, Component.text(targetCharacter.getLevel())));
    }

    private void handleCharacterCreate(Player player, HPlayer hPlayer, String[] args) {
        if (hPlayer.getCharacters().size() >= hPlayer.getMaximumCharacters()) {
            player.sendMessage(Component.translatable("hecate.character.selection.max_characters",
                Component.text(hPlayer.getMaximumCharacters())));
            player.sendMessage(Component.translatable("hecate.character.selection.max_characters_help"));
            return;
        }

        player.sendMessage(Component.translatable("hecate.character.selection.create_hint"));
        player.sendMessage(Component.translatable("hecate.character.selection.create_better_experience"));
    }

    private void handleCharacterInfo(Player player, HPlayer hPlayer, String[] args) {
        HCharacter character;

        if (args.length < 3) {
            // Show current character info
            character = hPlayer.getSelectedCharacter();
            if (character == null) {
                player.sendMessage(Component.translatable("hecate.commands.character.no_character_selected"));
                return;
            }
        } else {
            // Show specific character info
            int characterIndex;
            try {
                characterIndex = Integer.parseInt(args[2]) - 1;
            } catch (NumberFormatException e) {
                player.sendMessage(Component.translatable("hecate.commands.character.invalid_number"));
                return;
            }

            List<HCharacter> characters = hPlayer.getCharacters();
            if (characterIndex < 0 || characterIndex >= characters.size()) {
                player.sendMessage(Component.translatable("hecate.commands.character.number_out_of_range",
                    Component.text(characters.size())));
                return;
            }
            character = characters.get(characterIndex);
        }

        // Display character info
        Component className = character.getHClass() != null
            ? character.getHClass().getDisplayName()
            : Component.text("No Class", NamedTextColor.GRAY);
        String traitlineName = character.getTraitline() != null ? character.getTraitline().getName() : "None";

        player.sendMessage(Component.text("═══════════════════════════════════════", NamedTextColor.GOLD));
        player.sendMessage(Component.translatable("hecate.character.info.title"));
        player.sendMessage(Component.text("═══════════════════════════════════════", NamedTextColor.GOLD));
        player.sendMessage(Component.translatable("hecate.character.info.class", className));
        player.sendMessage(Component.translatable("hecate.character.info.level", Component.text(character.getLevel())));
        player.sendMessage(Component.translatable("hecate.character.info.traitline", Component.text(traitlineName)));
        player.sendMessage(Component.translatable("hecate.character.info.created",
            Component.text(dateFormat.format(character.getCreatedAt()))));

        if (character.getSkills() != null && !character.getSkills().isEmpty()) {
            player.sendMessage(Component.translatable("hecate.character.info.skills",
                Component.text(character.getSkills().size())));
        }

        if (player.hasPermission("hecate.admin")) {
            player.sendMessage(Component.text("ID: " + character.getCharacterID(), NamedTextColor.DARK_GRAY));
        }
        player.sendMessage(Component.text("═══════════════════════════════════════", NamedTextColor.GOLD));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 2) {
            List<String> subcommands = List.of("list", "select", "create", "info", "selection");
            String input = args[1].toLowerCase();
            for (String cmd : subcommands) {
                if (cmd.startsWith(input)) {
                    completions.add(cmd);
                }
            }
        } else if (args.length == 3 && (args[1].equalsIgnoreCase("select") || args[1].equalsIgnoreCase("info"))) {
            if (sender instanceof Player player) {
                HPlayer hPlayer = databaseManager.getHPlayer(player);
                if (hPlayer != null) {
                    List<HCharacter> characters = hPlayer.getCharacters();
                    for (int i = 0; i < characters.size(); i++) {
                        completions.add(String.valueOf(i + 1));
                    }
                }
            }
        }

        return completions;
    }
}