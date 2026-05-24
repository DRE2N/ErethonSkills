package de.erethon.hecate.arenas;

import java.util.UUID;

public record ArenaLeaderboardEntry(UUID playerId, double rating, double deviation) {
}
