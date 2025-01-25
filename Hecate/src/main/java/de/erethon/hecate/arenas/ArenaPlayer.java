package de.erethon.hecate.arenas;

import de.erethon.hecate.data.HCharacter;
import de.erethon.hecate.data.HPlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ArenaPlayer {

    private ConfigurationSection section;
    private final HPlayer hPlayer;
    private final Player bukkitPlayer;
    private Rating rating;
    private ArenaMatch currentMatch;
    private final List<PastArenaMatch> matchHistory = new ArrayList<>();

    public ArenaPlayer(HPlayer hPlayer, Player bukkitPlayer, ConfigurationSection section) {
        this.hPlayer = hPlayer;
        this.bukkitPlayer = bukkitPlayer;
        this.rating = new Rating(bukkitPlayer.getUniqueId());
        this.section = section;
        load();
    }

    public void recordMatch(ArenaMatch match) {
        Set<PastArenaTeam> teams = new HashSet<>();
        for (ArenaTeam team : match.getTeams()) {
            Set<Rating> ratings = new HashSet<>();
            for (ArenaPlayer player : team.getPlayers()) {
                ratings.add(player.getRating());
            }
            teams.add(new PastArenaTeam(team.getId(), ratings));
        }
        Set<Rating> ratings = new HashSet<>();
        for (ArenaPlayer player : match.getWinner().getPlayers()) {
            ratings.add(player.getRating());
        }
        PastArenaTeam winner = new PastArenaTeam(match.getWinner().getId(), ratings);
        PastArenaMatch pastMatch = new PastArenaMatch(teams, System.currentTimeMillis(), winner);
        matchHistory.add(pastMatch);
    }


    public Rating getRating() {
        return rating;
    }

    public HPlayer getPlayer() {
        return hPlayer;
    }

    public Player getBukkitPlayer() {
        return bukkitPlayer;
    }

    public HCharacter getCurrentCharacter() {
        return hPlayer.getSelectedCharacter();
    }

    public void recalculateRating() {
        if (matchHistory.isEmpty()) {
            return;
        }
        List<Rating> opponents = new ArrayList<>();
        List<Double> scores = new ArrayList<>();
        for (PastArenaMatch match : matchHistory) {
            boolean isWinner = false;
            for (Rating rating : match.winner().members()) {
                if (rating.getUUID() == this.rating.getUUID()) {
                    isWinner = true;
                    break;
                }
            }
            double score = isWinner ? 1.0 : 0.0;
            for (PastArenaTeam team : match.teams()) {
                if (team.id() == match.winner().id()) {
                    continue;
                }
                for (Rating rating : team.members()) {
                    opponents.add(rating);
                    scores.add(score);
                }
            }
        }
        GlickoConstants.updateRating(rating, opponents, scores);
    }

    private void load() {
        if (section == null) {
            return;
        }
        rating = new Rating(bukkitPlayer.getUniqueId(), section.getDouble("currentRating.rating"), section.getDouble("currentRating.deviation"), section.getDouble("currentRating.volatility"));
        ConfigurationSection history = section.getConfigurationSection("history");
        for (String key : history.getKeys(false)) {
            ConfigurationSection matchSection = history.getConfigurationSection(key);
            long time = matchSection.getLong("time");
            Set<PastArenaTeam> teams = new HashSet<>();
            for (String teamKey : matchSection.getKeys(false)) {
                ConfigurationSection teamSection = matchSection.getConfigurationSection(teamKey);
                Set<Rating> ratings = new HashSet<>();
                for (String playerKey : teamSection.getKeys(false)) {
                    ConfigurationSection playerSection = teamSection.getConfigurationSection(playerKey);
                    Rating rating = new Rating(UUID.fromString(playerSection.getString("uuid")), playerSection.getDouble("rating"), playerSection.getDouble("deviation"), playerSection.getDouble("volatility"));
                    ratings.add(rating);
                }
                teams.add(new PastArenaTeam(Integer.parseInt(teamKey), ratings));
            }
            PastArenaTeam winner = teams.stream().filter(t -> t.id() == matchSection.getInt("winner")).findFirst().orElse(null);
            PastArenaMatch match = new PastArenaMatch(teams, time, winner);
            matchHistory.add(match);
        }
    }

    public ConfigurationSection save() {
        ConfigurationSection section = new MemoryConfiguration();
        section.set("currentRating.rating", rating.getRating());
        section.set("currentRating.deviation", rating.getDeviation());
        section.set("currentRating.volatility", rating.getVolatility());
        ConfigurationSection history = section.createSection("history");
        int x = 0;
        for (PastArenaMatch match : matchHistory) {
            history.set(x + ".time", match.timestamp());
            history.set(x + ".winner", match.winner().id());
            for (PastArenaTeam team : match.teams()) {
                int i = team.id();
                for (Rating rating : team.members()) {
                    history.set(x + "." + i + ".uuid", rating.getUUID());
                    history.set(x + "." + i + ".rating", rating.getRating());
                    history.set(x + "." + i + ".deviation", rating.getDeviation());
                    history.set(x + "." + i + ".volatility", rating.getVolatility());
                }
            }
            x++;
        }
        return section;
    }

}
