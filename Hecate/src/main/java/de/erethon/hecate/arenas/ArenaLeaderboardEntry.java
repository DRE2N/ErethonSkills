package de.erethon.hecate.arenas;

import java.util.UUID;

public record ArenaLeaderboardEntry(UUID playerId, String playerName, double rating, double deviation) {
}
