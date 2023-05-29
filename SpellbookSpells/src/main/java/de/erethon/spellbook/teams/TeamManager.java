package de.erethon.spellbook.teams;

import org.bukkit.Color;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class TeamManager {

    public static NamespacedKey teamKey = new NamespacedKey("spellbook", "team");

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
                entity.getPersistentDataContainer().remove(teamKey);
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

    public void loadEntity(LivingEntity entity) {
        if (entity.getPersistentDataContainer().has(teamKey, PersistentDataType.STRING)) {
            String id = entity.getPersistentDataContainer().get(teamKey, PersistentDataType.STRING);
            SpellbookTeam team = getTeam(id);
            if (team != null) {
                teams.put(entity, team);
            }
        }
    }

    public void addEntityToTeam(LivingEntity entity, SpellbookTeam team) {
        teams.put(entity, team);
        entity.getPersistentDataContainer().set(teamKey, PersistentDataType.STRING, team.id());
    }

    public void removeEntityFromTeam(LivingEntity entity) {
        teams.remove(entity);
        entity.getPersistentDataContainer().remove(teamKey);
    }

    public boolean isInSameTeam(LivingEntity entity1, LivingEntity entity2) {
        return teams.get(entity1) == teams.get(entity2);
    }

}
