package de.erethon.hecate.arenas;

import java.util.HashSet;
import java.util.Set;

public class GameCoordinator {

    private final Set<ArenaMatch> runningMatches = new HashSet<>();

    public void startMatch(ArenaMatch match) {
        runningMatches.add(match);
    }

    public void endMatch(ArenaMatch match) {
        runningMatches.remove(match);
    }

    public Set<ArenaMatch> getRunningMatches() {
        return runningMatches;
    }

    public void recalculateRatings(Set<ArenaPlayer> players) {
        for (ArenaPlayer player : players) {
            player.recalculateRating();
        }
    }

}
