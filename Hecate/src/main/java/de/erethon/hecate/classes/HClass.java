package de.erethon.hecate.classes;

import de.erethon.hecate.Hecate;
import de.erethon.hecate.progression.LevelUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HClass extends YamlConfiguration {

    private final List<Traitline> traitlines = new ArrayList<>();
    private Map<Attribute, AttributeCurve> attributeCurves = Map.of();
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

    public Map<Attribute, AttributeCurve> getAttributeCurves() {
        return attributeCurves;
    }

    public Traitline getStarterTraitline() {
        Traitline firstUnlock = traitlines.stream()
                .min(Comparator.comparingInt(path -> Math.max(1, path.getInitialLevelRequirement())))
                .orElse(null);
        if (firstUnlock != null && (defaultTraitline == null ||
                defaultTraitline.getInitialLevelRequirement() > firstUnlock.getInitialLevelRequirement())) {
            return firstUnlock;
        }
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
        attributeCurves = loadAttributeCurves();

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
        if (defaultTraitline == null) {
            Hecate.log("Default discipline '" + getString("defaultTraitline")
                    + "' was not loaded for archetype " + id + " (" + file.getName() + ")");
        }
        color = TextColor.fromHexString(getString("color", "#ffffff"));
        List<String> configuredTraitlines = contains("disciplines")
                ? getStringList("disciplines")
                : getStringList("traitlines");
        for (String id : configuredTraitlines) {
            Traitline traitline = Hecate.getInstance().getTraitline(id);
            if (traitline == null) {
                Hecate.log("Unknown discipline '" + id + "' found in archetype file " + file.getName());
                continue;
            }
            traitlines.add(traitline);
        }
    }

    private Map<Attribute, AttributeCurve> loadAttributeCurves() {
        ConfigurationSection curves = getConfigurationSection("attributeCurves");
        if (curves == null) {
            if (contains("attributeCurves")) {
                Hecate.log("Expected an attributeCurves section in class " + id);
            }
            return Map.of();
        }
        Map<Attribute, AttributeCurve> loaded = new HashMap<>();
        for (String name : curves.getKeys(false)) {
            NamespacedKey key = NamespacedKey.fromString(name);
            Attribute attribute = key == null ? null : Registry.ATTRIBUTE.get(key);
            ConfigurationSection config = curves.getConfigurationSection(name);
            if (attribute == null || config == null) {
                Hecate.log("Invalid attribute curve '" + name + "' in class " + id);
                continue;
            }
            double base = config.getDouble("base", Double.NaN);
            double multiplier = config.contains("multiplier") ? config.getDouble("multiplier", Double.NaN) : 1.0;
            double growth = config.getDouble("growth", Double.NaN);
            double exponent = config.getDouble("exponent", Double.NaN);
            double scaledBase = base * multiplier;
            AttributeCurve curve = new AttributeCurve(scaledBase, growth, exponent);
            if (!Double.isFinite(scaledBase) || base < 0 || multiplier <= 0
                    || (attribute == Attribute.MAX_HEALTH && scaledBase <= 0)
                    || !Double.isFinite(growth) || growth < 0 || !Double.isFinite(exponent) || exponent <= 0
                    || !Double.isFinite(curve.valueAt(LevelUtil.MAX_CHARACTER_LEVEL))) {
                Hecate.log("Invalid base, multiplier, growth, or exponent for attribute curve '" + name + "' in class " + id);
                continue;
            }
            loaded.put(attribute, curve);
        }
        return Map.copyOf(loaded);
    }

    public record AttributeCurve(double base, double growth, double exponent) {

        public double valueAt(int level) {
            int levelsGained = Math.clamp(level, 1, LevelUtil.MAX_CHARACTER_LEVEL) - 1;
            return base * Math.pow(1.0 + growth * levelsGained, exponent);
        }
    }
}
