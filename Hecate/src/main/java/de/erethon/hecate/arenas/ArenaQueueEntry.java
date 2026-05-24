package de.erethon.hecate.arenas;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record ArenaQueueEntry(UUID id, List<UUID> playerIds, ArenaQueueType queueType, int teamSize, ArenaMode mode, long queuedAt) {

    public static ArenaQueueEntry of(List<Player> players, ArenaQueueType queueType, int teamSize, ArenaMode mode) {
        List<UUID> ids = new ArrayList<>();
        for (Player player : players) {
            ids.add(player.getUniqueId());
        }
        return new ArenaQueueEntry(UUID.randomUUID(), ids, queueType, teamSize, mode, System.currentTimeMillis());
    }

    public static ArenaQueueEntry combined(List<ArenaQueueEntry> entries, ArenaQueueType queueType, int teamSize, ArenaMode mode) {
        List<UUID> ids = new ArrayList<>();
        long queuedAt = System.currentTimeMillis();
        for (ArenaQueueEntry entry : entries) {
            ids.addAll(entry.playerIds());
            queuedAt = Math.min(queuedAt, entry.queuedAt());
        }
        return new ArenaQueueEntry(UUID.randomUUID(), ids, queueType, teamSize, mode, queuedAt);
    }
}
