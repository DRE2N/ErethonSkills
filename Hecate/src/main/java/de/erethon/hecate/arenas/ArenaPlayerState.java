package de.erethon.hecate.arenas;

import de.erethon.hecate.data.HCharacter;
import de.erethon.hecate.Hecate;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.teams.SpellbookTeam;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class ArenaPlayerState {

    private final Location location;
    private final GameMode gameMode;
    private final ItemStack[] storageContents;
    private final ItemStack[] armorContents;
    private final ItemStack offhand;
    private final double health;
    private final int foodLevel;
    private final float saturation;
    private final boolean wasInCastMode;
    private final SpellbookTeam spellbookTeam;
    private final HCharacter character;

    public ArenaPlayerState(Player player, HCharacter character) {
        this.location = player.getLocation().clone();
        this.gameMode = player.getGameMode();
        this.storageContents = player.getInventory().getStorageContents().clone();
        this.armorContents = player.getInventory().getArmorContents().clone();
        this.offhand = player.getInventory().getItemInOffHand() == null ? null : player.getInventory().getItemInOffHand().clone();
        this.health = player.getHealth();
        this.foodLevel = player.getFoodLevel();
        this.saturation = player.getSaturation();
        this.wasInCastMode = character.isInCastMode();
        this.spellbookTeam = Spellbook.getInstance().getTeamManager().getTeam(player);
        this.character = character;
    }

    public void restore(Player player) {
        player.getInventory().setStorageContents(storageContents);
        player.getInventory().setArmorContents(armorContents);
        player.getInventory().setItemInOffHand(offhand);
        player.setGameMode(gameMode);
        player.setFoodLevel(foodLevel);
        player.setSaturation(saturation);
        player.teleport(location);
        if (!player.isDead()) {
            player.setHealth(Math.min(health, player.getMaxHealth()));
        }
    }

    public void saveRecovery(File folder, UUID playerId) {
        if (!folder.exists() && !folder.mkdirs()) {
            Hecate.log("Unable to create arena recovery folder: " + folder.getAbsolutePath());
            return;
        }
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set("location", location);
        configuration.set("gameMode", gameMode.name());
        configuration.set("inventory.storage", new ArrayList<>(Arrays.asList(storageContents)));
        configuration.set("inventory.armor", new ArrayList<>(Arrays.asList(armorContents)));
        configuration.set("inventory.offhand", offhand);
        configuration.set("health", health);
        configuration.set("foodLevel", foodLevel);
        configuration.set("saturation", saturation);
        try {
            configuration.save(recoveryFile(folder, playerId));
        } catch (IOException e) {
            Hecate.log("Unable to save arena recovery data for " + playerId + ": " + e.getMessage());
        }
    }

    public static boolean restoreRecovery(File folder, Player player) {
        File file = recoveryFile(folder, player.getUniqueId());
        if (!file.exists()) {
            return false;
        }
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
        player.getInventory().setStorageContents(readItems(configuration.getList("inventory.storage"), 36));
        player.getInventory().setArmorContents(readItems(configuration.getList("inventory.armor"), 4));
        player.getInventory().setItemInOffHand(configuration.getItemStack("inventory.offhand"));
        try {
            player.setGameMode(GameMode.valueOf(configuration.getString("gameMode", GameMode.SURVIVAL.name())));
        } catch (IllegalArgumentException ignored) {
            player.setGameMode(GameMode.SURVIVAL);
        }
        player.setFoodLevel(configuration.getInt("foodLevel", 20));
        player.setSaturation((float) configuration.getDouble("saturation", 20));
        Location savedLocation = configuration.getLocation("location");
        if (savedLocation != null) {
            player.teleport(savedLocation);
        }
        if (!player.isDead()) {
            player.setHealth(Math.min(configuration.getDouble("health", player.getMaxHealth()), player.getMaxHealth()));
        }
        clearRecovery(folder, player.getUniqueId());
        return true;
    }

    public static void clearRecovery(File folder, UUID playerId) {
        File file = recoveryFile(folder, playerId);
        if (file.exists() && !file.delete()) {
            Hecate.log("Unable to delete arena recovery data for " + playerId);
        }
    }

    private static File recoveryFile(File folder, UUID playerId) {
        return new File(folder, playerId + ".yml");
    }

    private static ItemStack[] readItems(List<?> list, int size) {
        ItemStack[] items = new ItemStack[size];
        if (list == null) {
            return items;
        }
        for (int i = 0; i < Math.min(size, list.size()); i++) {
            Object item = list.get(i);
            if (item instanceof ItemStack stack) {
                items[i] = stack;
            }
        }
        return items;
    }

    public boolean wasInCastMode() {
        return wasInCastMode;
    }

    public HCharacter getCharacter() {
        return character;
    }

    public SpellbookTeam getSpellbookTeam() {
        return spellbookTeam;
    }
}
