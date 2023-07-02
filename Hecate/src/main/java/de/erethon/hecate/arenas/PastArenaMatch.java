package de.erethon.hecate.arenas;

import java.util.Set;

public record PastArenaMatch(Set<PastArenaTeam> teams, long timestamp, PastArenaTeam winner) {
}
