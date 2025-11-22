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

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.Objects;

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
    public final static long JOB_BASE_XP = 1000L;
    public final static double JOB_GROWTH_FACTOR = 1.1;
    public final static long MAX_JOB_XP = calculateMaxXp(20, JOB_BASE_XP, JOB_GROWTH_FACTOR);

    // Per-(owner,currency) queue to serialize XP updates and avoid race conditions
    private static final ConcurrentMap<XpKey, CompletableFuture<Void>> xpQueues = new ConcurrentHashMap<>();

    private static CompletableFuture<Integer> fetchLevel(UUID ownerId, OwnerType ownerType, String currency, double base, double growth) {
        return economyService.getBalance(ownerId, ownerType, currency)
                .thenApply(totalXp -> getLevelFromXp(totalXp, base, growth));
    }

    public static CompletableFuture<Integer> getCharacterLevel(HCharacter character) {
        return fetchLevel(character.getCharacterID(), OwnerType.CHARACTER, "xp_character", CHARACTER_BASE_XP, CHARACTER_GROWTH_FACTOR);
    }

    public static CompletableFuture<Integer> getAllianceLevel(HPlayer player) {
        return fetchLevel(player.getPlayerId(), OwnerType.PLAYER, "xp_alliance", ALLIANCE_BASE_XP, ALLIANCE_GROWTH_FACTOR);
    }

    public static CompletableFuture<Integer> getWorldSeekerLevel(HPlayer player) {
        return fetchLevel(player.getPlayerId(), OwnerType.PLAYER, "xp_exploration", WORLD_SEEKER_BASE_XP, WORLD_SEEKER_GROWTH_FACTOR);
    }

    public static CompletableFuture<Integer> getJobLevel(HCharacter character) {
        return fetchLevel(character.getCharacterID(), OwnerType.CHARACTER, "xp_job", JOB_BASE_XP, JOB_GROWTH_FACTOR);
    }

    public static void giveCharacterXp(HCharacter character, long amount) {
        giveXp(
                character.getCharacterID(),
                OwnerType.CHARACTER,
                "xp_character",
                amount,
                CHARACTER_BASE_XP,
                CHARACTER_GROWTH_FACTOR,
                MAX_CHARACTER_XP,
                character.getPlayer(),
                "XP",
                "character",
                "character",
                () -> Hecate.log("Character " + character.getCharacterID() + " leveled up.")
        );
    }

    public static void giveAllianceXp(HPlayer player, long amount) {
        giveXp(
                player.getPlayerId(),
                OwnerType.PLAYER,
                "xp_alliance",
                amount,
                ALLIANCE_BASE_XP,
                ALLIANCE_GROWTH_FACTOR,
                MAX_ALLIANCE_XP,
                player.getPlayer(),
                "Alliance XP",
                "player",
                "alliance",
                () -> Hecate.log("Player " + player.getPlayerId() + " leveled up in Alliance.")
        );
    }

    public static void giveWorldSeekerXp(HPlayer player, long amount) {
        giveXp(
                player.getPlayerId(),
                OwnerType.PLAYER,
                "xp_exploration",
                amount,
                WORLD_SEEKER_BASE_XP,
                WORLD_SEEKER_GROWTH_FACTOR,
                MAX_WORLD_SEEKER_XP,
                player.getPlayer(),
                "World Seeker XP",
                "player",
                "exploration",
                () -> Hecate.log("Player " + player.getPlayerId() + " leveled up in World Seeker.")
        );
    }

    public static void giveJobXp(HCharacter character, long amount) {
        giveXp(
                character.getCharacterID(),
                OwnerType.CHARACTER,
                "xp_job",
                amount,
                JOB_BASE_XP,
                JOB_GROWTH_FACTOR,
                MAX_JOB_XP,
                character.getPlayer(),
                "Job XP",
                "character",
                "job",
                () -> Hecate.log("Job " + character.getCharacterID() + " leveled up.")
        );
    }

    private static void giveXp(
            UUID ownerId,
            OwnerType ownerType,
            String currency,
            long amount,
            double baseXp,
            double growthFactor,
            long maxXp,
            Player player,
            String xpLabel,
            String targetLabel,
            String messageType,
            Runnable levelUpLoggerSupplier
    ) {
        if (amount <= 0) return;
        XpKey key = new XpKey(ownerId, ownerType, currency);
        xpQueues.compute(key, (k, tail) -> {
            CompletableFuture<Void> start = (tail == null) ? CompletableFuture.completedFuture(null) : tail;
            CompletableFuture<Void> next = start.thenCompose(ignored -> economyService.getBalance(ownerId, ownerType, currency)
                    .thenApply(currentXp -> {
                        long spaceLeft = maxXp - currentXp;
                        if (spaceLeft <= 0) {
                            return new XpUpdate(currentXp, currentXp, 0L, getLevelFromXp(currentXp, baseXp, growthFactor));
                        }
                        long xpToGive = Math.min(amount, spaceLeft);
                        int currentLevel = getLevelFromXp(currentXp, baseXp, growthFactor);
                        try {
                            economyService.deposit(ownerId, ownerType, currency, xpToGive, "Hecate", null);
                        } catch (Exception ex) {
                            Hecate.log("Failed to grant XP for " + targetLabel + " " + ownerId + ": " + ex.getMessage());
                        }
                        long updatedXp = currentXp + xpToGive;
                        return new XpUpdate(currentXp, updatedXp, xpToGive, currentLevel);
                    })
                    .thenAccept(result -> {
                        if (result == null) return;
                        long updatedXp = result.updatedXp();
                        int currentLevel = result.currentLevel();
                        int newLevel = getLevelFromXp(updatedXp, baseXp, growthFactor);

                        Hecate.log("Gave " + result.xpGiven() + " " + xpLabel + " to " + targetLabel + " " + ownerId + ". New total: " + updatedXp);
                        Hecate.log("Old level: " + currentLevel + ", new level: " + newLevel);

                        if (newLevel > currentLevel) {
                            levelUpLoggerSupplier.run();
                            if (player != null) {
                                Bukkit.getScheduler().runTask(Hecate.getInstance(), () ->
                                        LevelMessages.displayLevelMessage(
                                                player,
                                                newLevel,
                                                updatedXp,
                                                calculateMaxXp(newLevel + 1, baseXp, growthFactor),
                                                messageType
                                        ));
                            }
                        }
                        if (player != null) {
                            Bukkit.getScheduler().runTask(Hecate.getInstance(), () ->
                                    displayLevel(
                                            player,
                                            newLevel,
                                            getProgressForCurrentLevel(newLevel, updatedXp, baseXp, growthFactor),
                                            20 * 5
                                    ));
                        }
                    }));
            // Cleanup completed tails to prevent unbounded map growth
            next.whenComplete((v, ex) -> xpQueues.computeIfPresent(k, (kk, existing) -> existing == next ? null : existing));
            return next;
        });
    }

    // XP for a level X is calculated as:
    // Level 1 to 2: baseXp * (growthFactor ^ 0)
    // Level 2 to 3: baseXp * (growthFactor ^ 1)
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
        Currency jobLevel = new Currency(
                103,
                "xp_job",
                "Job XP",
                "Job XP",
                0L,
                false,
                Scope.CHARACTER
        );
        try {
            // Only does something if the currency does not already exist, so we can safely call this multiple times
            economyService.defineCurrency(characterLevel);
            economyService.defineCurrency(allianceLevel);
            economyService.defineCurrency(worldSeekerLevel);
            economyService.defineCurrency(jobLevel);
            Hecate.log("XP currencies created successfully.");
        } catch (Exception e) {
            Hecate.log("Failed to create currencies: " + e.getMessage());
        }
    }

    // Display the level for x seconds, and then revert to the character level, which is always displayed
    public static void displayLevel(Player player, int level, float progress, int duration) {
        HCharacter character = Hecate.getInstance().getDatabaseManager().getCurrentCharacter    (player);
        if (character == null) {
            return;
        }
        // Ensure we send packets on the main thread
        if (player != null) {
            Bukkit.getScheduler().runTask(Hecate.getInstance(), () -> sendFakeLevel(player, level, progress));
            Bukkit.getScheduler().runTaskLater(Hecate.getInstance(), () -> {
                displayCharLevel(player);
            }, duration);
        }
    }

    public static void displayCharLevel(Player player) {
        HCharacter character = Hecate.getInstance().getDatabaseManager().getCurrentCharacter(player);
        if (character == null) {
            return;
        }
        CompletableFuture<Long> xpFuture = economyService.getBalance(character.getCharacterID(), OwnerType.CHARACTER, "xp_character");
        xpFuture.thenAccept(totalXp -> {
            int currentLevel = getLevelFromXp(totalXp, CHARACTER_BASE_XP, CHARACTER_GROWTH_FACTOR);
            float currentProgress = getProgressForCurrentLevel(currentLevel, totalXp, CHARACTER_BASE_XP, CHARACTER_GROWTH_FACTOR);
            // Ensure packet send happens on the main thread
            if (player != null) {
                Bukkit.getScheduler().runTask(Hecate.getInstance(), () -> sendFakeLevel(player, currentLevel, currentProgress));
            }
        });
    }

    public static void sendFakeLevel(Player player, int level, float progress) {
        CraftPlayer craftPlayer = (CraftPlayer) player;
        ServerPlayer serverPlayer = craftPlayer.getHandle();
        ClientboundSetExperiencePacket packet = new ClientboundSetExperiencePacket(progress, Integer.MAX_VALUE, level);
        serverPlayer.connection.send(packet);
    }

    private static final class XpKey {
        private final UUID ownerId;
        private final OwnerType ownerType;
        private final String currency;
        private XpKey(UUID ownerId, OwnerType ownerType, String currency) {
            this.ownerId = ownerId;
            this.ownerType = ownerType;
            this.currency = currency;
        }
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            XpKey xpKey = (XpKey) o;
            return Objects.equals(ownerId, xpKey.ownerId) && ownerType == xpKey.ownerType && Objects.equals(currency, xpKey.currency);
        }
        @Override
        public int hashCode() {
            return Objects.hash(ownerId, ownerType, currency);
        }
    }

    private static final class XpUpdate {
        private final long previousXp;
        private final long updatedXp;
        private final long xpGiven;
        private final int currentLevel;
        private XpUpdate(long previousXp, long updatedXp, long xpGiven, int currentLevel) {
            this.previousXp = previousXp;
            this.updatedXp = updatedXp;
            this.xpGiven = xpGiven;
            this.currentLevel = currentLevel;
        }
        public long previousXp() { return previousXp; }
        public long updatedXp() { return updatedXp; }
        public long xpGiven() { return xpGiven; }
        public int currentLevel() { return currentLevel; }
    }

}
