package de.erethon.spellbook.teams;

import org.bukkit.Color;
import org.bukkit.entity.LivingEntity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class TeamManager {

    private Set<SpellbookTeam> existingTeams = new HashSet<>();
    private final HashMap<LivingEntity, SpellbookTeam> teams = new HashMap<>();

    public void createTeam(String id, String name, Color color) {
        existingTeams.add(new SpellbookTeam(id, name, color));
    }

    public void removeTeam(String id) {
        existingTeams.removeIf(team -> team.id().equals(id));
        for (LivingEntity entity : teams.keySet()) {
            if (teams.get(entity).id().equals(id)) {
                teams.remove(entity);
            }
        }
    }

    public SpellbookTeam getTeam(String id) {
        for (SpellbookTeam team : existingTeams) {
            if (team.id().equals(id)) {
                return team;
            }
        }
        return null;
    }

    public void addEntityToTeam(LivingEntity entity, SpellbookTeam team) {
        teams.put(entity, team);
    }

    public void removeEntityFromTeam(LivingEntity entity) {
        teams.remove(entity);
    }

    public boolean isInSameTeam(LivingEntity entity1, LivingEntity entity2) {
        return teams.get(entity1) == teams.get(entity2);
    }

}
