package de.erethon.hecate.arenas;

import java.util.HashSet;
import java.util.Set;

public class ArenaMatch {

    private final Set<ArenaTeam> teams = new HashSet<>();
    private boolean isOver = false;
    private ArenaTeam winner;

    public ArenaMatch(ArenaTeam... teams) {
        this.teams.addAll(Set.of(teams));
    }

    public Set<ArenaTeam> getTeams() {
        return teams;
    }

    public void win(ArenaTeam team) {
        if (isOver) {
            throw new IllegalStateException("Can't win a match that is already over.");
        }
        isOver = true;
        winner = team;
        for (ArenaTeam t : teams) {
            for (ArenaPlayer player : t.getPlayers()) {
                player.recordMatch(this);
            }
        }
    }

    public boolean isOver() {
        return isOver;
    }

    public ArenaTeam getWinner() {
        if (!isOver) {
            throw new IllegalStateException("Can't get winner of a match that isn't over.");
        }
        return winner;
    }
}
