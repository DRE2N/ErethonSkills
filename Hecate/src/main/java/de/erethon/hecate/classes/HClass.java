package de.erethon.hecate.classes;

import de.erethon.hecate.Hecate;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HClass extends YamlConfiguration {

    private final List<Traitline> traitlines = new ArrayList<>();

    private Traitline defaultTraitline = null;

    private String id;
    private String displayName;
    private TextColor color;
    private String description;

    // Equipment

    public HClass(File file) {
        try {
            load(file);
        } catch (IOException | InvalidConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Traitline> getTraitlines() {
        return traitlines;
    }

    public Traitline getStarterTraitline() {
        if (defaultTraitline == null) {
            Hecate.log("Class " + getId() + " has no default traitline configured");
            return null;
        }
        return defaultTraitline;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public TextColor getColor() {
        return color;
    }

    @SuppressWarnings("removal")
    @Override
    public void load(@NotNull File file) throws IOException, InvalidConfigurationException {
        super.load(file);
        id = file.getName().replace(".yml", "");
        displayName = getString("displayName", id);
        description = getString("description", "");
        defaultTraitline = Hecate.getInstance().getTraitline(getString("defaultTraitline"));
        color = TextColor.fromHexString(getString("color", "#ffffff"));
        for (String id : getStringList("traitlines")) {
            Traitline traitline = Hecate.getInstance().getTraitline(id);
            if (traitline == null) {
                Hecate.log("Unknown traitline '" + id + "' found under 'traitlines' in class file " + getName());
                continue;
            }
            traitlines.add(traitline);
        }
    }

}
