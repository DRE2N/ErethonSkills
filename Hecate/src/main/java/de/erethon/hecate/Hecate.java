package de.erethon.hecate;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.compatibility.Internals;
import de.erethon.bedrock.plugin.EPlugin;
import de.erethon.bedrock.plugin.EPluginSettings;
import de.erethon.hecate.casting.HPlayer;
import de.erethon.hecate.casting.HPlayerCache;
import de.erethon.hecate.classes.HClass;
import de.erethon.hecate.classes.Traitline;
import de.erethon.hecate.commands.HecateCommandCache;
import de.erethon.hecate.events.CombatModeReason;
import de.erethon.hecate.listeners.EntityListener;
import de.erethon.hecate.listeners.PlayerCastListener;
import de.erethon.hecate.ui.EntityStatusDisplayManager;
import de.erethon.hecate.util.SpellbookTranslator;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookAPI;
import de.erethon.spellbook.api.SpellbookSpell;
import de.erethon.spellbook.api.TraitData;
import de.erethon.spellbook.spells.SpellbookBaseSpell;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Main;
import net.minecraft.server.MinecraftServer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.Pig;
import org.bukkit.event.HandlerList;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class Hecate extends EPlugin {

    private static Hecate instance;

    public static File PLAYERS;
    public static File INVENTORIES;
    public static File CLASSES;
    public static File TRAITLINES;

    private Spellbook spellbook;
    private HPlayerCache hPlayerCache;
    private HecateCommandCache commands;
    private EntityStatusDisplayManager statusDisplayManager;
    private final Set<Traitline> traitlines = new HashSet<>();
    private final Set<HClass> hClasses = new HashSet<>();

    public boolean ready = false;
    private final GlobalTranslator translator = GlobalTranslator.translator();
    private final SpellbookTranslator reg = new SpellbookTranslator();

    public Hecate() {
        settings = EPluginSettings.builder()
                .internals(Internals.v1_18_R2)
                .forcePaper(true)
                .economy(true)
                .build();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        instance = this;
        loadCore();
    }

    public void loadCore() {
        spellbook = new Spellbook(this);
        MessageUtil.log("Loading spells...");
        for (String spell : spellbook.getAPI().getLibrary().getLoaded().keySet()) {
            MessageUtil.log("- " + spell);
        }
        initFolders();
        instantiate();
        registerCommands();
        getServer().getPluginManager().registerEvents(new PlayerCastListener(), this);
        getServer().getPluginManager().registerEvents(new EntityListener(), this);
        Bukkit.getScheduler().runTaskLater(this, () -> { // Workaround for Spellbook not loading spells on load
            Bukkit.getServer().getSpellbookAPI().getLibrary().reload();
            loadTraitlines();
            loadClasses();
            registerTranslations();
            translator.addSource(reg);
            createPlaceholderDefinitions(Bukkit.getWorlds().get(0));
            ready = true;
        }, 30);
    }

    private void registerTranslations() {
        for (SpellData data : spellbook.getAPI().getLibrary().getLoaded().values()) {
            if (data.contains("name")) {
                ConfigurationSection nameSection = data.getConfigurationSection("name");
                if (nameSection == null) {
                    MessageUtil.log("Spell " + data.getId() + " has no name.");
                    continue;
                }
                for (String key : nameSection.getKeys(false)) {
                    String value = nameSection.getString(key, "<no translation>");
                    Locale locale;
                    if (key.contains("de")) {
                        locale = Locale.GERMANY;
                    } else {
                        locale = Locale.US;
                    }
                    reg.registerTranslation("spellbook.spell.name." + data.getId(), value, locale);
                }
            }
            if (data.contains("description")) {
                ConfigurationSection descriptionSection = data.getConfigurationSection("description");
                if (descriptionSection == null) {
                    MessageUtil.log("Spell " + data.getId() + " has no description.");
                    continue;
                }
                int maxLineCount = 0;  // We need to know this for lore rendering
                for (String key : descriptionSection.getKeys(false)) {
                    List<String> value = descriptionSection.getStringList(key);
                    Locale locale;
                    if (key.contains("de")) {
                        locale = Locale.GERMANY;
                    } else {
                        locale = Locale.US;
                    }
                    int i = 0;
                    for (String line : value) {
                        reg.registerTranslation("spellbook.spell.description." + data.getId() + "." + i, line, locale);
                        MessageUtil.log("Registered translation for " + data.getId() + " line " + i + " in " + locale + ": " + line);
                        i++;
                    }
                    maxLineCount = Math.max(maxLineCount, i);
                }
                data.setDescriptionLineCount(maxLineCount);
            }
        }
        for (TraitData data : spellbook.getAPI().getLibrary().getLoadedTraits().values()) {
            if (data.contains("name")) {
                ConfigurationSection nameSection = data.getConfigurationSection("name");
                if (nameSection == null) {
                    MessageUtil.log("Trait " + data.getId() + " has no name.");
                    continue;
                }
                for (String key : nameSection.getKeys(false)) {
                    String value = nameSection.getString(key, "<no translation>");
                    Locale locale;
                    if (key.contains("de")) {
                        locale = Locale.GERMANY;
                    } else {
                        locale = Locale.US;
                    }
                    reg.registerTranslation("spellbook.trait.name." + data.getId(), value, locale);
                }
            }
            if (data.contains("description")) {
                ConfigurationSection descriptionSection = data.getConfigurationSection("description");
                if (descriptionSection == null) {
                    MessageUtil.log("Trait " + data.getId() + " has no description.");
                    continue;
                }
                int maxLineCount = 0;
                for (String key : descriptionSection.getKeys(false)) {
                    List<String> value = descriptionSection.getStringList(key);
                    Locale locale;
                    if (key.contains("de")) {
                        locale = Locale.GERMANY;
                    } else {
                        locale = Locale.US;
                    }
                    int i = 0;
                    for (String line : value) {
                        reg.registerTranslation("spellbook.trait.description." + data.getId() + "." + i, line, locale);
                        i++;
                    }
                    maxLineCount = Math.max(maxLineCount, i);
                }
                data.setDescriptionLineCount(maxLineCount);
            }
        }
        translator.addSource(reg);
    }

    private void createPlaceholderDefinitions(World world ) {
        MessageUtil.log("Creating placeholder definitions...");
        Pig pig = world.spawn(world.getSpawnLocation(), Pig.class);
        pig.setPersistent(false);
        pig.setInvisible(true);
        for (SpellData data : spellbook.getAPI().getLibrary().getLoaded().values()) {
            // We need some entity
            SpellbookSpell spell = data.getActiveSpell(pig);
            if (spell != null) {
                spell.getPlaceholders(pig);
                if (spell instanceof SpellbookBaseSpell baseSpell) {
                    List<String> placeholders = new ArrayList<>(baseSpell.getPlaceholderNames());
                    data.set("availablePlaceholders", placeholders);
                    data.setComments("availablePlaceholders", List.of("This list contains all placeholders that can be used in this spell.",
                            "You can use those placeholders in the description with",
                            "<arg:(index of placeholder)>. Example: <arg:1> for the first placeholder."));
                }
                try {
                    data.save(data.getFile());
                } catch (IOException e) {
                    MessageUtil.log("Failed to save spell data for " + data.getId() + " after creating placeholder definitions.");
                    throw new RuntimeException(e);
                }
            }
        }
        pig.remove();
        MessageUtil.log("Done. Created placeholder definitions for " + spellbook.getAPI().getLibrary().getLoaded().size() + " spells.");
    }

    @Override
    public void onDisable() {
        for (HPlayer hPlayer : hPlayerCache.getPlayers()) { // TODO: This does not seem to include all players?
            if (hPlayer.getSelectedCharacter().isInCastmode()) {
                hPlayer.getSelectedCharacter().switchMode(CombatModeReason.PLUGIN);
            }
            hPlayer.saveUser();
        }
        // Duh duh duh
        Bukkit.getScheduler().cancelTasks(this);
        HandlerList.unregisterAll(this);
    }

    private void loadClasses() {
        for (File file : CLASSES.listFiles()) {
            if (file.getName().endsWith(".yml")) {
                try {
                    hClasses.add(new HClass(file));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        MessageUtil.log("Loaded " + hClasses.size() + " classes.");
    }

    private void loadTraitlines() {
        for (File file : TRAITLINES.listFiles()) {
            if (file.getName().endsWith(".yml")) {
                try {
                    traitlines.add(new Traitline(file));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        MessageUtil.log("Loaded " + traitlines.size() + " traitlines.");
    }

    public void initFolders() {
        initFolder(getDataFolder());
        initFolder(PLAYERS = new File(getDataFolder(), "players"));
        initFolder(INVENTORIES = new File(getDataFolder(), "inventories"));
        initFolder(CLASSES = new File(getDataFolder(), "classes"));
        initFolder(TRAITLINES = new File(getDataFolder(), "traitlines"));
    }

    public void initFolder(File folder) {
        if (!folder.exists()) {
            folder.mkdir();
        }
    }

    public void instantiate() {
        hPlayerCache = new HPlayerCache(this);
        statusDisplayManager = new EntityStatusDisplayManager();
        Server server = Bukkit.getServer();
        CraftServer craftServer = (CraftServer) server;
        MinecraftServer minecraftServer = craftServer.getServer();
        minecraftServer.registryAccess().registryOrThrow(Registries.BIOME);
    }

    public void registerCommands() {
        setCommandCache(commands = new HecateCommandCache(this));
        commands.register(this);
    }

    /* getter */

    public Spellbook getSpellbook() {
        return spellbook;
    }

    public SpellbookAPI getAPI() {
        return spellbook.getAPI();
    }

    public HPlayerCache getHPlayerCache() {
        return hPlayerCache;
    }

    public HecateCommandCache getCommands() {
        return commands;
    }

    public static Hecate getInstance() {
        return instance;
    }

    public Set<Traitline> getTraitlines() {
        return traitlines;
    }

    public Set<HClass> getHClasses() {
        return hClasses;
    }

    public EntityStatusDisplayManager getStatusDisplayManager() {
        return statusDisplayManager;
    }

    public Traitline getTraitline(String id) {
        for (Traitline traitline : traitlines) {
            if (traitline.getId().equalsIgnoreCase(id)) {
                return traitline;
            }
        }
        return null;
    }

    public HClass getHClass(String id) {
        for (HClass hClass : hClasses) {
            if (hClass.getId().equalsIgnoreCase(id)) {
                return hClass;
            }
        }
        return null;
    }

}
