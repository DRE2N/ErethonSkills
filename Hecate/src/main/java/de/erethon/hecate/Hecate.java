package de.erethon.hecate;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.compatibility.Internals;
import de.erethon.bedrock.database.BedrockDBConnection;
import de.erethon.bedrock.plugin.EPlugin;
import de.erethon.bedrock.plugin.EPluginSettings;
import de.erethon.hecate.charselection.CharacterLobby;
import de.erethon.hecate.data.DatabaseManager;
import de.erethon.hecate.classes.HClass;
import de.erethon.hecate.classes.Traitline;
import de.erethon.hecate.commands.HecateCommandCache;
import de.erethon.hecate.items.HEquipmentManager;
import de.erethon.hecate.listeners.EntityListener;
import de.erethon.hecate.listeners.EquipmentListener;
import de.erethon.hecate.listeners.PlayerCastListener;
import de.erethon.hecate.ui.EntityStatusDisplayManager;
import de.erethon.hecate.util.SpellbookTranslator;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookAPI;
import de.erethon.spellbook.api.SpellbookSpell;
import de.erethon.spellbook.api.TraitData;
import de.erethon.spellbook.spells.SpellbookBaseSpell;
import net.kyori.adventure.translation.GlobalTranslator;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.Pig;
import org.bukkit.event.HandlerList;

import java.io.File;
import java.io.IOException;
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
    private HecateCommandCache commands;
    private DatabaseManager databaseManager;
    private HEquipmentManager equipmentManager;
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
        YamlConfiguration config = YamlConfiguration.loadConfiguration(new File(Bukkit.getWorldContainer(), "environment.yml"));
        try {
            BedrockDBConnection connection = new BedrockDBConnection(config.getString("dbUrl"),
                    config.getString("dbUser"),
                    config.getString("dbPassword"),
                    "de.erethon.bedrock.postgresql.ds.PGSimpleDataSource");
            databaseManager = new DatabaseManager(connection);
        }
        catch (Exception e) {
            Hecate.log("Failed to connect to database. Hecate will not work.");
            e.printStackTrace();
            return;
        }
        File equipmentFile = new File(getDataFolder(), "equipment.yml");
        initFolders();
        instantiate();
        registerCommands();
        getServer().getPluginManager().registerEvents(new PlayerCastListener(), this);
        getServer().getPluginManager().registerEvents(new EntityListener(), this);
        getServer().getPluginManager().registerEvents(new EquipmentListener(), this);
        Bukkit.getScheduler().runTaskLater(this, () -> { // Workaround for Spellbook not loading spells on load
            Bukkit.getServer().getSpellbookAPI().getLibrary().reload();
            loadTraitlines();
            loadClasses();
            registerTranslations();
            translator.addSource(reg);
            equipmentManager = new HEquipmentManager(equipmentFile);
            createPlaceholderDefinitions(Bukkit.getWorlds().get(0));
            ready = true;
        }, 30);
    }

    private void registerTranslations() {
        for (SpellData data : spellbook.getAPI().getLibrary().getLoaded().values()) {
            if (data.contains("name")) {
                ConfigurationSection nameSection = data.getConfigurationSection("name");
                if (nameSection == null) {
                    Hecate.log("Spell " + data.getId() + " has no name.");
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
                    Hecate.log("Spell " + data.getId() + " has no description.");
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
                    Hecate.log("Trait " + data.getId() + " has no name.");
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
                    Hecate.log("Trait " + data.getId() + " has no description.");
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
        Hecate.log("Creating placeholder definitions...");
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
                    int num = 0;
                    for (String placeholder : placeholders) {
                        data.set("availablePlaceholders." + num, placeholder);
                        num++;
                    }
                    data.setComments("availablePlaceholders", List.of("This list contains all placeholders that can be used in this spell.",
                            "You can use those placeholders in the description with",
                            "<arg:(index of placeholder)>. Example: <arg:1> for the first placeholder."));
                }
                try {
                    data.save(data.getFile());
                } catch (IOException e) {
                    Hecate.log("Failed to save spell data for " + data.getId() + " after creating placeholder definitions.");
                    throw new RuntimeException(e);
                }
            }
        }
        pig.remove();
        Hecate.log("Done. Created placeholder definitions for " + spellbook.getAPI().getLibrary().getLoaded().size() + " spells.");
    }

    @Override
    public void onDisable() {
        Bukkit.getScheduler().cancelTasks(this);
        HandlerList.unregisterAll(this);
        databaseManager.close();
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
        Hecate.log("Loaded " + hClasses.size() + " classes.");
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
        Hecate.log("Loaded " + traitlines.size() + " traitlines.");
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
        statusDisplayManager = new EntityStatusDisplayManager();
        Server server = Bukkit.getServer();
        CraftServer craftServer = (CraftServer) server;
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

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public HEquipmentManager getEquipmentManager() {
        return equipmentManager;
    }

    public CharacterLobby getLobbyInUse() {
        return new CharacterLobby("default");
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
        Hecate.log("Traitline " + id + " not found.");
        return null;
    }

    public HClass getHClass(String id) {
        for (HClass hClass : hClasses) {
            if (hClass.getId().equalsIgnoreCase(id)) {
                return hClass;
            }
        }
        if (id.equalsIgnoreCase("default")) {
            return hClasses.stream().findFirst().orElse(null);
        }
        return null;
    }

    public static void log(String message) {
        Hecate.getInstance().getLogger().info(message);
    }

}
