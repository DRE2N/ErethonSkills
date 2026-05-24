package de.erethon.hecate.arenas;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

public class PayloadDefinition {

    private final List<ArenaLocation> nodes = new ArrayList<>();
    private final List<Integer> checkpoints = new ArrayList<>();
    private double radius = 5.0;
    private int roundSeconds = 300;
    private int overtimeSeconds = 20;
    private double blocksPerSecond = 1.25;

    public static PayloadDefinition load(ConfigurationSection section) {
        PayloadDefinition definition = new PayloadDefinition();
        if (section == null) {
            return definition;
        }
        definition.radius = section.getDouble("radius", definition.radius);
        definition.roundSeconds = section.getInt("roundSeconds", definition.roundSeconds);
        definition.overtimeSeconds = section.getInt("overtimeSeconds", definition.overtimeSeconds);
        definition.blocksPerSecond = section.getDouble("blocksPerSecond", definition.blocksPerSecond);
        ConfigurationSection nodesSection = section.getConfigurationSection("nodes");
        if (nodesSection != null) {
            for (String key : nodesSection.getKeys(false)) {
                ArenaLocation location = ArenaLocation.load(nodesSection.getConfigurationSection(key));
                if (location != null) {
                    definition.nodes.add(location);
                }
            }
        }
        definition.checkpoints.addAll(section.getIntegerList("checkpoints"));
        return definition;
    }

    public void save(ConfigurationSection section) {
        section.set("radius", radius);
        section.set("roundSeconds", roundSeconds);
        section.set("overtimeSeconds", overtimeSeconds);
        section.set("blocksPerSecond", blocksPerSecond);
        section.set("checkpoints", checkpoints);
        ConfigurationSection nodesSection = section.createSection("nodes");
        for (int i = 0; i < nodes.size(); i++) {
            nodes.get(i).save(nodesSection.createSection(String.valueOf(i)));
        }
    }

    public List<ArenaLocation> getNodes() {
        return nodes;
    }

    public List<Integer> getCheckpoints() {
        return checkpoints;
    }

    public double getRadius() {
        return radius;
    }

    public int getRoundSeconds() {
        return roundSeconds;
    }

    public int getOvertimeSeconds() {
        return overtimeSeconds;
    }

    public double getBlocksPerSecond() {
        return blocksPerSecond;
    }
}
