package de.erethon.hecate.arenas;

import java.util.UUID;

public record ArenaRatingChange(UUID playerId, double before, double after, double deviation) {

    public double delta() {
        return after - before;
    }
}
