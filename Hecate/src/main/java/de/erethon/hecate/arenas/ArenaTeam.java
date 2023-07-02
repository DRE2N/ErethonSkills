package de.erethon.hecate.arenas;

import java.util.Set;

public class ArenaTeam {

    private Set<ArenaPlayer> players;
    private int id;

    public ArenaTeam(int id, ArenaPlayer... players) {
        this.players = Set.of(players);
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public Set<ArenaPlayer> getPlayers() {
        return players;
    }

    public void addPlayer(ArenaPlayer player) {
        players.add(player);
    }

    public void removePlayer(ArenaPlayer player) {
        players.remove(player);
    }

}
