package de.erethon.spellbook.teams;

import de.erethon.bedrock.chat.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class TeamManager {

    public static NamespacedKey teamKey = new NamespacedKey("spellbook", "team");

    private final Set<SpellbookTeam> existingTeams = new HashSet<>();
    private final HashMap<UUID, SpellbookTeam> teams = new HashMap<>();

    public void createTeam(String id, String name, Color color) {
        existingTeams.add(new SpellbookTeam(id, name, color));
        MessageUtil.log("Registered team " + id + " with name " + name + " and color " + color.toString() + ".");
    }

    public void removeTeam(String id) {
        existingTeams.removeIf(team -> team.id().equals(id));
        for (UUID uuid : teams.keySet()) {
            Entity entity = Bukkit.getEntity(uuid);
            if (teams.get(uuid).id().equals(id)) {
                teams.remove(uuid);
                if (entity != null) {
                    entity.getPersistentDataContainer().remove(teamKey);
                }
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
            if (team == null) {
                createTeam(id, id, Color.WHITE);
            }
            teams.put(entity.getUniqueId(), team);
        }
    }

    public void addEntityToTeam(LivingEntity entity, SpellbookTeam team) {
        teams.put(entity.getUniqueId(), team);
        entity.getPersistentDataContainer().set(teamKey, PersistentDataType.STRING, team.id());
    }

    public void removeEntityFromTeam(LivingEntity entity) {
        teams.remove(entity.getUniqueId());
        entity.getPersistentDataContainer().remove(teamKey);
    }

    public boolean isInSameTeam(LivingEntity entity1, LivingEntity entity2) {
        return teams.get(entity1.getUniqueId()) == teams.get(entity2.getUniqueId());
    }

}
