package de.erethon.hecate.arenas;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class ArenaDefinition {

    private final String id;
    private String displayName;
    private boolean enabled;
    private ArenaMode mode = ArenaMode.CONTROL;
    private int teamSize = 1;
    private ArenaLocation lobby;
    private final Map<Integer, ArenaLocation> teamSpawns = new LinkedHashMap<>();
    private final Map<String, CapturePointDefinition> capturePoints = new LinkedHashMap<>();
    private PayloadDefinition payload = new PayloadDefinition();
    private int controlScoreLimit = 500;
    private int controlTimeSeconds = 600;
    private int controlPointsPerKill = 0;
    private File file;

    public ArenaDefinition(String id) {
        this.id = id.toLowerCase();
        this.displayName = id;
    }

    public static ArenaDefinition load(File file) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ArenaDefinition definition = new ArenaDefinition(yaml.getString("id", file.getName().replace(".yml", "")));
        definition.file = file;
        definition.enabled = yaml.getBoolean("enabled", false);
        definition.displayName = yaml.getString("displayName", definition.id);
        definition.mode = ArenaMode.parse(yaml.getString("mode", "control"));
        if (definition.mode == null) {
            definition.mode = ArenaMode.CONTROL;
        }
        definition.teamSize = Math.max(1, Math.min(5, yaml.getInt("teamSize", 1)));
        definition.lobby = ArenaLocation.load(yaml.getConfigurationSection("lobby"));
        ConfigurationSection spawns = yaml.getConfigurationSection("teamSpawns");
        if (spawns != null) {
            for (String key : spawns.getKeys(false)) {
                definition.teamSpawns.put(Integer.parseInt(key), ArenaLocation.load(spawns.getConfigurationSection(key)));
            }
        }
        ConfigurationSection points = yaml.getConfigurationSection("control.points");
        definition.controlScoreLimit = Math.max(1, yaml.getInt("control.scoreLimit", 500));
        definition.controlTimeSeconds = Math.max(1, yaml.getInt("control.timeSeconds", 600));
        definition.controlPointsPerKill = Math.max(0, yaml.getInt("control.pointsPerKill", 0));
        if (points != null) {
            for (String key : points.getKeys(false)) {
                CapturePointDefinition point = CapturePointDefinition.load(key, points.getConfigurationSection(key));
                if (point != null) {
                    definition.capturePoints.put(key, point);
                }
            }
        }
        definition.payload = PayloadDefinition.load(yaml.getConfigurationSection("escort.payload"));
        return definition;
    }

    public void save(File arenasFolder) throws IOException {
        if (file == null) {
            file = new File(arenasFolder, id + ".yml");
        }
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("id", id);
        yaml.set("enabled", enabled);
        yaml.set("displayName", displayName);
        yaml.set("mode", mode.name().toLowerCase());
        yaml.set("teamSize", teamSize);
        if (lobby != null) {
            lobby.save(yaml.createSection("lobby"));
        }
        ConfigurationSection spawns = yaml.createSection("teamSpawns");
        for (Map.Entry<Integer, ArenaLocation> entry : teamSpawns.entrySet()) {
            entry.getValue().save(spawns.createSection(String.valueOf(entry.getKey())));
        }
        ConfigurationSection points = yaml.createSection("control.points");
        yaml.set("control.scoreLimit", controlScoreLimit);
        yaml.set("control.timeSeconds", controlTimeSeconds);
        yaml.set("control.pointsPerKill", controlPointsPerKill);
        for (CapturePointDefinition point : capturePoints.values()) {
            point.save(points.createSection(point.id()));
        }
        payload.save(yaml.createSection("escort.payload"));
        yaml.save(file);
    }

    public boolean isUsableFor(ArenaMode requestedMode, int requestedTeamSize) {
        return enabled && teamSize == requestedTeamSize && (requestedMode == null || mode == requestedMode) && validate().isEmpty();
    }

    public String validate() {
        if (lobby == null) {
            return "missing lobby";
        }
        if (!teamSpawns.containsKey(0) || !teamSpawns.containsKey(1)) {
            return "missing team spawns";
        }
        if (mode == ArenaMode.CONTROL && capturePoints.isEmpty()) {
            return "missing capture points";
        }
        if (mode == ArenaMode.ESCORT && payload.getNodes().size() < 2) {
            return "missing payload path";
        }
        return "";
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public ArenaMode getMode() { return mode; }
    public void setMode(ArenaMode mode) { this.mode = mode; }
    public int getTeamSize() { return teamSize; }
    public void setTeamSize(int teamSize) { this.teamSize = Math.max(1, Math.min(5, teamSize)); }
    public ArenaLocation getLobby() { return lobby; }
    public void setLobby(ArenaLocation lobby) { this.lobby = lobby; }
    public Map<Integer, ArenaLocation> getTeamSpawns() { return teamSpawns; }
    public Collection<CapturePointDefinition> getCapturePoints() { return capturePoints.values(); }
    public Map<String, CapturePointDefinition> getCapturePointMap() { return capturePoints; }
    public PayloadDefinition getPayload() { return payload; }
    public int getControlScoreLimit() { return controlScoreLimit; }
    public void setControlScoreLimit(int controlScoreLimit) { this.controlScoreLimit = Math.max(1, controlScoreLimit); }
    public int getControlTimeSeconds() { return controlTimeSeconds; }
    public void setControlTimeSeconds(int controlTimeSeconds) { this.controlTimeSeconds = Math.max(1, controlTimeSeconds); }
    public int getControlPointsPerKill() { return controlPointsPerKill; }
    public void setControlPointsPerKill(int controlPointsPerKill) { this.controlPointsPerKill = Math.max(0, controlPointsPerKill); }
}
