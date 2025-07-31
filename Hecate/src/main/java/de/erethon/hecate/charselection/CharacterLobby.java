package de.erethon.hecate.charselection;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.hecate.Hecate;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CharacterLobby {

    private final Hecate plugin = Hecate.getInstance();

    private String id = "";
    private Location origin;
    private List<Location> pedestalLocations = new ArrayList<>();

    public CharacterLobby(String id, Location origin) {
        this.id = id;
        this.origin = origin;
        save();
    }

    public CharacterLobby(String id) {
        File file = new File(plugin.getDataFolder(), "lobbies/" + id + ".yml");
        if (!file.exists()) {
            Hecate.log("Character lobby " + id + " does not exist.");
            return;
        }
        this.id = id;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        load(config);
    }

    public CharacterSelection createSelectionForPlayer(Player player) {
        return new CharacterSelection(player, this);
    }

    public void addPedestal(Location location) {
        pedestalLocations.add(location);
        save();
    }

    public void removePedestalCloseTo(Location location) {
        pedestalLocations.removeIf(loc -> loc.distance(location) < 1);
        save();
    }

    public List<Location> getPedestalLocations() {
        return pedestalLocations;
    }

    public Location getOrigin() {
        return origin;
    }

    private void load(YamlConfiguration config) {
        id = config.getString("id");
        origin = (Location) config.get("origin");
        List<String> pedestalStrings = config.getStringList("pedestalLocations");
        if (pedestalStrings.isEmpty()) {
            return;
        }
        for (String s : pedestalStrings) {
            String[] split = s.split(",");
            Location location = new Location(plugin.getServer().getWorld(split[0]), Double.parseDouble(split[1]), Double.parseDouble(split[2]), Double.parseDouble(split[3]), Float.parseFloat(split[4]), Float.parseFloat(split[5]));
            pedestalLocations.add(location);
        }
    }

    private void save() {
        File folder = new File(plugin.getDataFolder(), "lobbies");
        if (!folder.exists()) {
            folder.mkdirs();
        }
        File file = new File(folder, id + ".yml");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        config.set("id", id);
        config.set("origin", origin);
        List<String> pedestalStrings = new ArrayList<>();
        for (Location loc : pedestalLocations) {
            pedestalStrings.add(loc.getWorld().getName() + "," + loc.getX() + "," + loc.getY() + "," + loc.getZ() + "," + loc.getYaw() + "," + loc.getPitch());
        }
        config.set("pedestalLocations", pedestalStrings);
        try {
            config.save(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
