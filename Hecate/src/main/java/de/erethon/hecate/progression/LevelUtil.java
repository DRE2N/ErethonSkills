package de.erethon.hecate.progression;

import de.erethon.hecate.Hecate;
import de.erethon.hecate.data.HCharacter;
import de.erethon.hecate.data.HPlayer;
import de.erethon.tyche.EconomyService;
import de.erethon.tyche.TychePlugin;
import de.erethon.tyche.models.Currency;
import de.erethon.tyche.models.OwnerType;
import de.erethon.tyche.models.Scope;
import net.minecraft.network.protocol.game.ClientboundSetExperiencePacket;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;

public class LevelUtil {

    private final static TychePlugin tychePlugin = (TychePlugin) Bukkit.getPluginManager().getPlugin("Tyche");
    private final static EconomyService economyService = tychePlugin.getEco();

    public final static double CHARACTER_BASE_XP = 500.0;
    public final static double CHARACTER_GROWTH_FACTOR = 1.15;
    public final static long MAX_CHARACTER_XP = calculateMaxXp(20, CHARACTER_BASE_XP, CHARACTER_GROWTH_FACTOR);
    public final static double ALLIANCE_BASE_XP = 5000.0;
    public final static double ALLIANCE_GROWTH_FACTOR = 1.12;
    public final static long MAX_ALLIANCE_XP = calculateMaxXp(20, ALLIANCE_BASE_XP, ALLIANCE_GROWTH_FACTOR);
    public final static double WORLD_SEEKER_BASE_XP = 1500.0;
    public final static double WORLD_SEEKER_GROWTH_FACTOR = 1.05;
    public final static long MAX_WORLD_SEEKER_XP = calculateMaxXp(20, WORLD_SEEKER_BASE_XP, WORLD_SEEKER_GROWTH_FACTOR);

    public static CompletableFuture<Integer> getCharacterLevel(HCharacter character) {
        CompletableFuture<Long> xpFuture = economyService.getBalance(character.getCharacterID(), OwnerType.CHARACTER, "xp_character");
        return xpFuture.thenApply(totalXp -> getLevelFromXp(totalXp, CHARACTER_BASE_XP, CHARACTER_GROWTH_FACTOR));
    }

    public static CompletableFuture<Integer> getAllianceLevel(HPlayer player) {
        CompletableFuture<Long> xpFuture = economyService.getBalance(player.getPlayerId(), OwnerType.PLAYER, "xp_alliance");
        return xpFuture.thenApply(totalXp -> getLevelFromXp(totalXp, ALLIANCE_BASE_XP, ALLIANCE_GROWTH_FACTOR));
    }

    public static CompletableFuture<Integer> getWorldSeekerLevel(HPlayer player) {
        CompletableFuture<Long> xpFuture = economyService.getBalance(player.getPlayerId(), OwnerType.PLAYER, "xp_exploration");
        return xpFuture.thenApply(totalXp -> getLevelFromXp(totalXp, WORLD_SEEKER_BASE_XP, WORLD_SEEKER_GROWTH_FACTOR));
    }

    public static void giveCharacterXp(HCharacter character, long amount) {
        if (amount <= 0) {
            return;
        }
        CompletableFuture<Long> xpFuture = economyService.getBalance(character.getCharacterID(), OwnerType.CHARACTER, "xp_character");
        xpFuture.thenAccept(currentXp -> {
            long newXp = currentXp + amount;
            long xpToGive = amount;
            if (newXp > MAX_CHARACTER_XP) {
                xpToGive = MAX_CHARACTER_XP - currentXp;
                newXp = MAX_CHARACTER_XP;
            }
            int currentLevel = getLevelFromXp(currentXp, CHARACTER_BASE_XP, CHARACTER_GROWTH_FACTOR);
            economyService.deposit(character.getCharacterID(), OwnerType.CHARACTER, "xp_character", xpToGive, "Hecate", null);
            Hecate.log("Gave " + xpToGive + " XP to character " + character.getCharacterID() + ". New total: " + newXp);
            int newLevel = getLevelFromXp(newXp, CHARACTER_BASE_XP, CHARACTER_GROWTH_FACTOR);
            if (newLevel > currentLevel) {
                Hecate.log("Character " + character.getCharacterID() + " leveled up from " + currentLevel + " to " + newLevel);
                LevelMessages.displayLevelMessage(character.getPlayer(), newLevel, currentXp, currentXp + getXpForNextLevel(newLevel + 1, CHARACTER_BASE_XP, CHARACTER_GROWTH_FACTOR), "character");
            }
            displayLevel(character.getPlayer(), newLevel, getProgressForCurrentLevel(newLevel, newXp, CHARACTER_BASE_XP, CHARACTER_GROWTH_FACTOR), 20 * 5);
        });
    }

    public static void giveAllianceXp(HPlayer player, long amount) {
        if (amount <= 0) {
            return;
        }
        CompletableFuture<Long> xpFuture = economyService.getBalance(player.getPlayerId(), OwnerType.PLAYER, "xp_alliance");
        xpFuture.thenAccept(currentXp -> {
            long newXp = currentXp + amount;
            long xpToGive = amount;
            if (newXp > MAX_ALLIANCE_XP) {
                xpToGive = MAX_ALLIANCE_XP - currentXp;
                newXp = MAX_ALLIANCE_XP;
            }
            int currentLevel = getLevelFromXp(currentXp, ALLIANCE_BASE_XP, ALLIANCE_GROWTH_FACTOR);
            economyService.deposit(player.getPlayerId(), OwnerType.PLAYER, "xp_alliance", xpToGive, "Hecate", null);
            Hecate.log("Gave " + xpToGive + " Alliance XP to player " + player.getPlayerId() + ". New total: " + newXp);
            int newLevel = getLevelFromXp(newXp, ALLIANCE_BASE_XP, ALLIANCE_GROWTH_FACTOR);
            if (newLevel > currentLevel) {
                Hecate.log("Player " + player.getPlayerId() + " leveled up in Alliance from " + currentLevel + " to " + newLevel);
                LevelMessages.displayLevelMessage(player.getPlayer(), newLevel, currentXp, currentXp + getXpForNextLevel(newLevel + 1, ALLIANCE_BASE_XP, ALLIANCE_GROWTH_FACTOR), "alliance");
            }
            displayLevel(player.getPlayer(), newLevel, getProgressForCurrentLevel(newLevel, newXp, ALLIANCE_BASE_XP, ALLIANCE_GROWTH_FACTOR), 20 * 5);
        });
    }

    public static void giveWorldSeekerXp(HPlayer player, long amount) {
        if (amount <= 0) {
            return;
        }
        CompletableFuture<Long> xpFuture = economyService.getBalance(player.getPlayerId(), OwnerType.PLAYER, "xp_exploration");
        xpFuture.thenAccept(currentXp -> {
            long newXp = currentXp + amount;
            long xpToGive = amount;
            if (newXp > MAX_WORLD_SEEKER_XP) {
                xpToGive = MAX_WORLD_SEEKER_XP - currentXp;
                newXp = MAX_WORLD_SEEKER_XP;
            }
            int currentLevel = getLevelFromXp(currentXp, WORLD_SEEKER_BASE_XP, WORLD_SEEKER_GROWTH_FACTOR);
            economyService.deposit(player.getPlayerId(), OwnerType.PLAYER, "xp_exploration", xpToGive, "Hecate", null);
            Hecate.log("Gave " + xpToGive + " World Seeker XP to player " + player.getPlayerId() + ". New total: " + newXp);
            int newLevel = getLevelFromXp(newXp, WORLD_SEEKER_BASE_XP, WORLD_SEEKER_GROWTH_FACTOR);
            if (newLevel > currentLevel) {
                Hecate.log("Player " + player.getPlayerId() + " leveled up in World Seeker from " + currentLevel + " to " + newLevel);
                LevelMessages.displayLevelMessage(player.getPlayer(), newLevel, currentXp, currentXp + getXpForNextLevel(newLevel + 1, WORLD_SEEKER_BASE_XP, WORLD_SEEKER_GROWTH_FACTOR), "exploration");
            }

            displayLevel(player.getPlayer(), newLevel, getProgressForCurrentLevel(newLevel, newXp, WORLD_SEEKER_BASE_XP, WORLD_SEEKER_GROWTH_FACTOR), 20 * 5);
        });
    }

    public static int getLevelFromXp(long totalXp, double baseXp, double growthFactor) {
        if (totalXp < 0 || baseXp <= 0 || growthFactor <= 1.0) {
            return 1;
        }

        int currentLevel = 1;
        long xpForNextLevel = (long) Math.floor(baseXp);

        while (totalXp >= xpForNextLevel) {
            totalXp -= xpForNextLevel;
            currentLevel++;
            // Level L to L+1: baseXp * (growthFactor ^ (L-1))
            xpForNextLevel = (long) Math.floor(baseXp * Math.pow(growthFactor, currentLevel - 1));
        }

        return currentLevel;
    }

    public static long calculateMaxXp(int maxLevel, double baseXp, double growthFactor) {
        if (maxLevel <= 1 || baseXp <= 0 || growthFactor <= 1.0) {
            return 0;
        }

        long totalCumulativeXp = 0;
        for (int currentLevel = 1; currentLevel < maxLevel; currentLevel++) {
            totalCumulativeXp += (long) Math.floor(baseXp * Math.pow(growthFactor, currentLevel - 1));
        }
        return totalCumulativeXp;
    }

    public static long getXpForNextLevel(int level, double baseXp, double growthFactor) {
        if (level < 1 || baseXp <= 0 || growthFactor <= 1.0) {
            return 0;
        }
        // Level L to L+1: baseXp * (growthFactor ^ (L-1))
        return (long) Math.floor(baseXp * Math.pow(growthFactor, level - 1));
    }

    public static float getProgressForCurrentLevel(int level, long totalXp, double baseXp, double growthFactor) {
        long xpNeededToReachCurrentLevel = calculateMaxXp(level, baseXp, growthFactor);

        long xpEarnedInCurrentLevel = totalXp - xpNeededToReachCurrentLevel;

        long xpCostForThisLevel = getXpForNextLevel(level, baseXp, growthFactor);

        float progress = 0.0f;
        if (xpCostForThisLevel > 0) {
            progress = (float) Math.min(0.999999, (double) xpEarnedInCurrentLevel / xpCostForThisLevel);
        }
        return progress;
    }

    public static void createCurrencies() {
        Currency characterLevel = new Currency(
                100,
                "xp_character",
                "Character XP",
                "Character XP",
                0L,
                false,
                Scope.CHARACTER
        );
        Currency allianceLevel = new Currency(
                101,
                "xp_alliance",
                "Alliance XP",
                "Alliance XP",
                0L,
                false,
                Scope.PLAYER
        );
        Currency worldSeekerLevel = new Currency(
                102,
                "xp_exploration",
                "World Seeker XP",
                "World Seeker XP",
                0L,
                false,
                Scope.PLAYER
        );
        try {
            // Only does something if the currency does not already exist, so we can safely call this multiple times
            economyService.defineCurrency(characterLevel);
            economyService.defineCurrency(allianceLevel);
            economyService.defineCurrency(worldSeekerLevel);
            Hecate.log("XP currencies created successfully.");
        } catch (Exception e) {
            Hecate.log("Failed to create currencies: " + e.getMessage());
        }
    }

    // Display the level for x seconds, and then revert to the character level, which is always displayed
    public static void displayLevel(Player player, int level, float progress, int duration) {
        HCharacter character = Hecate.getInstance().getDatabaseManager().getCurrentCharacter(player);
        if (character == null) {
            return;
        }
        sendFakeLevel(player, level, progress);
        Bukkit.getScheduler().runTaskLater(Hecate.getInstance(), () -> {
            CompletableFuture<Long> xpFuture = economyService.getBalance(character.getCharacterID(), OwnerType.CHARACTER, "xp_character");
            xpFuture.thenAccept(totalXp -> {
                int currentLevel = getLevelFromXp(totalXp, CHARACTER_BASE_XP, CHARACTER_GROWTH_FACTOR);
                float currentProgress = getProgressForCurrentLevel(currentLevel, totalXp, CHARACTER_BASE_XP, CHARACTER_GROWTH_FACTOR);
                sendFakeLevel(player, currentLevel, currentProgress);
            });
        }, duration);
    }

    public static void sendFakeLevel(Player player, int level, float progress) {
        CraftPlayer craftPlayer = (CraftPlayer) player;
        ServerPlayer serverPlayer = craftPlayer.getHandle();
        ClientboundSetExperiencePacket packet = new ClientboundSetExperiencePacket(progress, Integer.MAX_VALUE, level);
        serverPlayer.connection.send(packet);
    }

}
