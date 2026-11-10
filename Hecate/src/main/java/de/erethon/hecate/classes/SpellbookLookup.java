package de.erethon.hecate.classes;

import de.erethon.hecate.Hecate;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellLibrary;
import de.erethon.spellbook.api.SpellbookAPI;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.configuration.InvalidConfigurationException;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Resolves Spellbook entries and repairs partial library loads one entry at a time.
 * Papyrus currently aborts the remainder of a library reload when any one config
 * fails, so otherwise valid entries later in the directory traversal stay absent.
 */
public final class SpellbookLookup {

    private SpellbookLookup() {
    }

    public static void reloadLibrary() {
        SpellbookAPI api = SpellbookAPI.getInstance();
        SpellLibrary library = api.getLibrary();
        ClassLoader classLoader = Hecate.getInstance().getClass().getClassLoader();
        api.getQueue().clear();
        library.getLoadedEffects().clear();
        library.getLoaded().clear();
        library.getLoadedTraits().clear();

        for (File config : findConfigs(new File(SpellbookAPI.SPELLS, "effects"))) {
            String id = configId(config);
            try {
                EffectData data = new EffectData(api, id, classLoader);
                data.load(config);
                library.getLoadedEffects().put(id, data);
            } catch (IOException | InvalidConfigurationException | RuntimeException e) {
                logLoadFailure("effect", id, config, e);
            }
        }

        for (File config : findConfigs(new File(SpellbookAPI.SPELLS, "spells"))) {
            String id = configId(config);
            try {
                SpellData data = new SpellData(api, id, classLoader);
                data.load(config);
                library.getLoaded().put(id, data);
            } catch (IOException | InvalidConfigurationException | RuntimeException e) {
                logLoadFailure("spell", id, config, e);
            }
        }

        List<File> traitConfigs = findConfigs(new File(SpellbookAPI.SPELLS, "traits"));
        for (File config : traitConfigs) {
            String id = configId(config);
            library.getLoadedTraits().put(id, new TraitData(api, id, classLoader));
        }
        for (File config : traitConfigs) {
            String id = configId(config);
            try {
                library.getLoadedTraits().get(id).load(config);
            } catch (IOException | InvalidConfigurationException | RuntimeException e) {
                library.getLoadedTraits().remove(id);
                logLoadFailure("trait", id, config, e);
            }
        }

        Hecate.log("Loaded Spellbook library in dependency order: "
                + library.getLoadedEffects().size() + " effects, "
                + library.getLoaded().size() + " spells, "
                + library.getLoadedTraits().size() + " traits.");
    }

    static @Nullable SpellData spell(String id) {
        SpellbookAPI api = SpellbookAPI.getInstance();
        SpellLibrary library = api.getLibrary();
        SpellData loaded = library.getSpellByID(id);
        if (loaded != null) {
            return loaded;
        }

        File config = findConfig(new File(SpellbookAPI.SPELLS, "spells"), id);
        if (config == null) {
            logMissing("spell", id, library.getLoaded().size());
            return null;
        }

        try {
            SpellData recovered = new SpellData(api, id, Hecate.getInstance().getClass().getClassLoader());
            recovered.load(config);
            library.getLoaded().put(id, recovered);
            Hecate.log("Recovered spell '" + id + "' after a partial Spellbook library load.");
            return recovered;
        } catch (IOException | InvalidConfigurationException | RuntimeException e) {
            Hecate.log("Failed to recover spell '" + id + "' from " + config.getPath() + ": " + e.getMessage());
            return null;
        }
    }

    static @Nullable TraitData trait(String id) {
        SpellbookAPI api = SpellbookAPI.getInstance();
        SpellLibrary library = api.getLibrary();
        TraitData loaded = library.getTraitByID(id);
        if (loaded != null) {
            return loaded;
        }

        File config = findConfig(new File(SpellbookAPI.SPELLS, "traits"), id);
        if (config == null) {
            logMissing("trait", id, library.getLoadedTraits().size());
            return null;
        }

        try {
            TraitData recovered = new TraitData(api, id, Hecate.getInstance().getClass().getClassLoader());
            recovered.load(config);
            library.getLoadedTraits().put(id, recovered);
            Hecate.log("Recovered trait '" + id + "' after a partial Spellbook library load.");
            return recovered;
        } catch (IOException | InvalidConfigurationException | RuntimeException e) {
            Hecate.log("Failed to recover trait '" + id + "' from " + config.getPath() + ": " + e.getMessage());
            return null;
        }
    }

    private static @Nullable File findConfig(File directory, String id) {
        File[] files = directory.listFiles();
        if (files == null) {
            return null;
        }
        File caseInsensitiveMatch = null;
        for (File file : files) {
            if (file.isDirectory()) {
                File nested = findConfig(file, id);
                if (nested != null) {
                    if (nested.getName().equals(id + ".yml")) {
                        return nested;
                    }
                    caseInsensitiveMatch = nested;
                }
                continue;
            }
            if (file.getName().equals(id + ".yml")) {
                return file;
            }
            if (file.getName().equalsIgnoreCase(id + ".yml")) {
                caseInsensitiveMatch = file;
            }
        }
        return caseInsensitiveMatch;
    }

    private static List<File> findConfigs(File directory) {
        List<File> configs = new ArrayList<>();
        File[] files = directory.listFiles();
        if (files == null) {
            return configs;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                configs.addAll(findConfigs(file));
            } else if (file.getName().endsWith(".yml")) {
                configs.add(file);
            }
        }
        return configs;
    }

    private static String configId(File config) {
        return config.getName().substring(0, config.getName().length() - ".yml".length());
    }

    private static void logLoadFailure(String type, String id, File config, Exception exception) {
        Hecate.log("Failed to load Spellbook " + type + " '" + id + "' from "
                + config.getPath() + ": " + exception.getMessage());
    }

    private static void logMissing(String type, String id, int loadedCount) {
        Hecate.log("Unknown " + type + " '" + id + "': no matching YAML exists under "
                + SpellbookAPI.SPELLS.getPath() + " (" + loadedCount + " " + type + " entries loaded).");
    }
}
