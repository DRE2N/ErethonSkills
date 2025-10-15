package de.erethon.hecate.classes;

import de.erethon.hecate.Hecate;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HClass extends YamlConfiguration {

    private final List<Traitline> traitlines = new ArrayList<>();
    MiniMessage mm = MiniMessage.miniMessage();

    private Traitline defaultTraitline = null;

    private String id;
    private Component displayName;
    private Component description;
    private TextColor color;

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

    public Component getDisplayName() {
        return displayName;
    }

    public Component getDescription() {
        return description;
    }

    public TextColor getColor() {
        return color;
    }

    @SuppressWarnings("removal")
    @Override
    public void load(@NotNull File file) throws IOException, InvalidConfigurationException {
        super.load(file);
        final de.erethon.spellbook.utils.SpellbookTranslator translator = Hecate.getInstance().getTranslator();
        id = file.getName().replace(".yml", "");

        if (contains("displayName")) {
            ConfigurationSection nameSection = getConfigurationSection("displayName");
            if (nameSection != null) {
                for (String key : nameSection.getKeys(false)) {
                    String value = nameSection.getString(key, "<no translation>");
                    java.util.Locale locale;
                    if (key.contains("de")) {
                        locale = java.util.Locale.GERMANY;
                    } else {
                        locale = java.util.Locale.US;
                    }
                    translator.registerTranslation("hecate.class." + id + ".name", value, locale);
                }
            }
        }
        displayName = Component.translatable("hecate.class." + id + ".name");

        if (contains("description")) {
            ConfigurationSection descriptionSection = getConfigurationSection("description");
            if (descriptionSection != null) {
                for (String key : descriptionSection.getKeys(false)) {
                    String value = descriptionSection.getString(key, "<no translation>");
                    java.util.Locale locale;
                    if (key.contains("de")) {
                        locale = java.util.Locale.GERMANY;
                    } else {
                        locale = java.util.Locale.US;
                    }
                    translator.registerTranslation("hecate.class." + id + ".description", value, locale);
                }
            }
        }
        description = Component.translatable("hecate.class." + id + ".description");

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
