package de.erethon.hecate;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.compatibility.Internals;
import de.erethon.bedrock.plugin.EPlugin;
import de.erethon.bedrock.plugin.EPluginSettings;
import de.erethon.hecate.casting.HPlayerCache;
import de.erethon.hecate.classes.HClass;
import de.erethon.hecate.classes.Traitline;
import de.erethon.hecate.commands.HecateCommandCache;
import de.erethon.hecate.listeners.EntityListener;
import de.erethon.hecate.listeners.PlayerCastListener;
import de.erethon.hecate.ui.EntityStatusDisplayManager;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellbookAPI;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public final class Hecate extends EPlugin {

    private static Hecate instance;

    public static File PLAYERS;
    public static File CLASSES;
    public static File TRAITLINES;

    private Spellbook spellbook;
    private HPlayerCache hPlayerCache;
    private HecateCommandCache commands;
    private EntityStatusDisplayManager statusDisplayManager;
    private final Set<Traitline> traitlines = new HashSet<>();
    private final Set<HClass> hClasses = new HashSet<>();

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
        }, 30);
    }

    @Override
    public void onDisable() {
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
