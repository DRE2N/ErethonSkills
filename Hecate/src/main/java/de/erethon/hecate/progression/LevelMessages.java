package de.erethon.hecate.progression;

import de.erethon.hecate.Hecate;
import de.erethon.hecate.data.HCharacter;
import de.erethon.spellbook.utils.SpellbookTranslator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class LevelMessages extends YamlConfiguration {

    private final SpellbookTranslator translator = Hecate.getInstance().getTranslator();
    private final static MiniMessage mm = MiniMessage.miniMessage();

    private static final Map<Integer, String> allianceLevelMessages = new HashMap<>();
    private static final Map<Integer, String> explorationLevelMessages = new HashMap<>();

    @Override
    public void load(@NotNull File file) throws FileNotFoundException, IOException, InvalidConfigurationException {
        super.load(file);
        if (contains("allianceLevelMessages")) {
            for (String outerKey : getConfigurationSection("allianceLevelMessages").getKeys(false)) {
                int level = Integer.parseInt(outerKey);
                ConfigurationSection messageSection = getConfigurationSection("allianceLevelMessages." + outerKey);
                if (messageSection == null) {
                    continue;
                }
                for (String key : messageSection.getKeys(false)) {
                    if (key.equals("de")) {
                        translator.registerTranslation("alliancelvl." + level, messageSection.getString(key), Locale.GERMANY);
                    } else {
                        translator.registerTranslation("alliancelvl." + level, messageSection.getString(key), Locale.US);
                    }
                }
                allianceLevelMessages.put(level, "alliancelvl." + level);
            }
            Hecate.log("Loaded " + allianceLevelMessages.size() + " alliance level messages");
        }
        if (contains("explorationLevelMessages")) {
            for (String outerKey : getConfigurationSection("explorationLevelMessages").getKeys(false)) {
                int level = Integer.parseInt(outerKey);
                ConfigurationSection messageSection = getConfigurationSection("explorationLevelMessages." + outerKey);
                if (messageSection == null) {
                    continue;
                }
                for (String key : messageSection.getKeys(false)) {
                    if (key.equals("de")) {
                        translator.registerTranslation("explorationlvl." + level, messageSection.getString(key), Locale.GERMANY);
                    } else {
                        translator.registerTranslation("explorationlvl." + level, messageSection.getString(key), Locale.US);
                    }
                }
                explorationLevelMessages.put(level, "explorationlvl." + level);
            }
            Hecate.log("Loaded " + explorationLevelMessages.size() + " character level messages");
        }
    }

    public static void displayLevelMessage(Player player, int level, long currentXp, long nextLevelXp, String type) {
        String messageKey = null;
        HCharacter character = Hecate.getInstance().getDatabaseManager().getCurrentCharacter(player);
        switch (type.toLowerCase(Locale.ROOT)) {
            case "character":
                if (character == null) {
                    return;
                }
                var traitline = character.getTraitline();
                LevelInfo info = null;
                if (traitline != null && traitline.getLevelInfo() != null) {
                    info = traitline.getLevelInfo().get(level);
                }
                messageKey = (info != null) ? info.messageTranslationKey() : null;
                break;
            case "alliance":
                messageKey = allianceLevelMessages.get(level);
                break;
            case "exploration":
                messageKey = explorationLevelMessages.get(level);
                break;
            case "job":
                if (character == null) {
                    return;
                }
                messageKey = "Not.defined."; // Needs JXL stuff
                break;
            default:
                return;
        }
        if (messageKey == null) {
            messageKey = "hecate.missing.levelmsg";
        }

        final String resolvedMessageKey = messageKey;
        final int newLevel = level;
        final long curXp = currentXp;
        final long nextXp = nextLevelXp;
        final String resolvedType = type;
        new BukkitRunnable() {
            @Override
            public void run() {
                Component message = Component.empty();
                Component levelMessage = Component.translatable(resolvedMessageKey);
                Component header = mm.deserialize("<gold><st>          </st> <yellow>Level Up! <gold><st>          <reset>");
                Component newLevelAndXp = mm.deserialize("<yellow>Level <gold>" + newLevel + "<dark_gray> | <gold>" + curXp + "<dark_gray>/<gold>" + nextXp + "<gray> XP");
                message = message.append(header).append(Component.newline()).append(newLevelAndXp).append(Component.newline()).append(Component.newline()).append(levelMessage);
                player.sendMessage(message);

                player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.RECORDS, 1.0f, 1.0f);
                Firework firework = player.getWorld().spawn(player.getLocation().add(0,7,0), Firework.class, fw -> {
                    fw.setTicksToDetonate(20);
                    FireworkMeta meta = fw.getFireworkMeta();
                    meta.addEffect(FireworkEffect.builder().flicker(false).withColor(Color.ORANGE).build());
                    fw.setFireworkMeta(meta);
                    fw.setSilent(true);
                });
                firework.setVelocity(new Vector(0, 0.5, 0));
                firework.detonate();
                // Heal the player if they are low and set food level on level up, as a little bonus
                double healthPercent = player.getHealth() / player.getMaxHealth();
                if (healthPercent <= 0.5) {
                    player.setHealth(player.getMaxHealth() * 0.5);
                }
                player.setFoodLevel(20);
                player.setSaturation(20);
                HCharacter character = Hecate.getInstance().getDatabaseManager().getCurrentCharacter(player);
                if (character != null && resolvedType.equalsIgnoreCase("character")) {
                    character.getCastingManager().setAttributesForLevel(newLevel); // Ensure we update the attributes for the new level
                }
            }
        }.runTask(Hecate.getInstance());
    }
}
