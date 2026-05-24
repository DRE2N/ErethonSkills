package de.erethon.hecate.arenas;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ArenaTeamEntry {

    private final int id;
    private final List<UUID> players;

    public ArenaTeamEntry(int id, List<UUID> players) {
        this.id = id;
        this.players = new ArrayList<>(players);
    }

    public int getId() {
        return id;
    }

    public List<UUID> getPlayers() {
        return players;
    }

    public boolean contains(UUID uuid) {
        return players.contains(uuid);
    }
}
