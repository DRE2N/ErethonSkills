// CharacterCommand.java
package de.erethon.hecate.commands;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.command.ECommand;
import de.erethon.hecate.Hecate;
import de.erethon.hecate.classes.HClass;
import de.erethon.hecate.classes.Traitline;
import de.erethon.hecate.data.HCharacter;
import de.erethon.hecate.data.HPlayer;
import de.erethon.hecate.data.DatabaseManager;
import de.erethon.hecate.events.PlayerSelectedCharacterEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class CharacterCommand extends ECommand implements TabCompleter {

    private final DatabaseManager databaseManager;

    public CharacterCommand() {
        this.databaseManager = Hecate.getInstance().getDatabaseManager();
        setCommand("character");
        setAliases("char");
        setMinArgs(0);
        setMaxArgs(3);
        setPlayerCommand(true);
        setConsoleCommand(false);
        setHelp("Char select");
    }

    public void onExecute(String[] args, CommandSender commandSender) {
        Player player = (Player) commandSender;
        HPlayer hPlayer = databaseManager.getHPlayer(player);

        if (hPlayer == null) {
            MessageUtil.sendMessage(commandSender, "<red>Player data not loaded.");
            return;
        }

        if (args.length == 1) {
            // List characters
            List<HCharacter> characters = hPlayer.getCharacters();
            if (characters.isEmpty()) {
                MessageUtil.sendMessage(commandSender, "<yellow>No characters found.");
            } else {
                MessageUtil.sendMessage(commandSender, "<green>Characters:");
                for (HCharacter character : characters) {
                    MessageUtil.sendMessage(commandSender, "<green>- " + character.getCharacterID());
                }
            }
        } else if (args.length == 4 && args[1].equalsIgnoreCase("create")) {
            // Create a new character
            UUID characterId = UUID.randomUUID();
            Traitline traitline = Hecate.getInstance().getTraitline(args[3]);
            HClass hClass = Hecate.getInstance().getHClass(args[2]);
            if (hClass == null) {
                MessageUtil.sendMessage(commandSender, "<red>Class not found.");
                return;
            }
            if (traitline == null) {
                MessageUtil.sendMessage(commandSender, "<red>Traitline not found.");
                return;
            }
            HCharacter newCharacter = new HCharacter(characterId, hPlayer, 1, args[2], new Timestamp(System.currentTimeMillis()), new ArrayList<>());
            newCharacter.setTraitline(traitline);
            hPlayer.getCharacters().add(newCharacter);
            newCharacter.saveToDatabase();
            MessageUtil.sendMessage(commandSender, "<green>Character created with ID " + characterId + "." + " Class: " + hClass.getId() + ", Traitline: " + traitline.getId());
            PlayerSelectedCharacterEvent event = new PlayerSelectedCharacterEvent(hPlayer, newCharacter, true);
            Bukkit.getPluginManager().callEvent(event);
        } else if (args.length == 3 && args[1].equalsIgnoreCase("switch")) {
            // Switch character
            UUID characterId = UUID.fromString(args[2]);
            if (hPlayer.getSelectedCharacter() != null && hPlayer.getSelectedCharacter().getCharacterID().equals(characterId)) {
                MessageUtil.sendMessage(commandSender, "<yellow>Character already selected.");
                return;
            }
            CompletableFuture<HCharacter> characterFuture = databaseManager.getCharacterData(characterId);
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 9999, 1, true, false, false));
            Title title = Title.title(Component.empty(), Component.text("Switching...", NamedTextColor.YELLOW));
            player.showTitle(title);
            characterFuture.thenApply(character -> {
                if (character != null) {
                    try {
                        hPlayer.setSelectedCharacter(character, false);
                        BukkitRunnable mainTask = new BukkitRunnable() {
                            @Override
                            public void run() {
                                PlayerSelectedCharacterEvent event = new PlayerSelectedCharacterEvent(hPlayer, character, false);
                                Bukkit.getPluginManager().callEvent(event);
                                player.removePotionEffect(PotionEffectType.BLINDNESS);
                                Title title = Title.title(Component.empty(), Component.empty());
                                player.showTitle(title);
                            }
                        };
                        mainTask.runTaskLater(Hecate.getInstance(), 20);
                        MessageUtil.sendMessage(commandSender, "<green>Switched to character with ID " + characterId + ".");
                    }
                    catch (Exception e) {
                        MessageUtil.sendMessage(commandSender, "<red>Error switching character: " + e.getMessage());
                        e.printStackTrace();
                    }
                } else {
                    MessageUtil.sendMessage(commandSender, "<red>Character not found.");
                }
                return null;
            });
        } else {
            MessageUtil.sendMessage(commandSender, "<red>Invalid command usage.");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 0) {
            completions.add("create");
            completions.add("switch");
        } else if (args[1].equalsIgnoreCase("switch")) {
            if (sender instanceof Player player) {
                HPlayer hPlayer = databaseManager.getHPlayer(player);
                if (hPlayer != null) {
                    for (HCharacter character : hPlayer.getCharacters()) {
                        completions.add(character.getCharacterID().toString());
                    }
                }
            }
        }
        return completions;
    }
}