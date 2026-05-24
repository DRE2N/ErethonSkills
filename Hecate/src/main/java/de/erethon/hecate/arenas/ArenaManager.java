package de.erethon.hecate.arenas;

import de.erethon.hecate.Hecate;
import de.erethon.hecate.data.HCharacter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ArenaManager implements Listener {

    public static final int ARENA_LEVEL = 20;

    private final Hecate plugin;
    private final File arenasFolder;
    private final File recoveryFolder;
    private final File weaponsFile;
    private final Map<String, ArenaDefinition> arenas = new HashMap<>();
    private final Map<String, ItemStack> traitlineWeapons = new HashMap<>();
    private final Map<UUID, ArenaSetupSession> setupSessions = new HashMap<>();
    private final Map<UUID, ArenaMatch> playerMatches = new HashMap<>();
    private final List<ArenaMatch> activeMatches = new ArrayList<>();
    private final ArenaQueueManager queueManager;

    public ArenaManager(Hecate plugin) {
        this.plugin = plugin;
        this.arenasFolder = new File(plugin.getDataFolder(), "arenas");
        this.recoveryFolder = new File(plugin.getDataFolder(), "arena-recovery");
        this.weaponsFile = new File(plugin.getDataFolder(), "arena-weapons.yml");
        this.queueManager = new ArenaQueueManager(this);
        if (!arenasFolder.exists()) {
            arenasFolder.mkdirs();
        }
        if (!recoveryFolder.exists()) {
            recoveryFolder.mkdirs();
        }
        loadArenas();
        loadWeapons();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void loadArenas() {
        arenas.clear();
        File[] files = arenasFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return;
        }
        for (File file : files) {
            ArenaDefinition definition = ArenaDefinition.load(file);
            arenas.put(definition.getId(), definition);
        }
        Hecate.log("Loaded " + arenas.size() + " arenas.");
    }

    public void loadWeapons() {
        traitlineWeapons.clear();
        if (!weaponsFile.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(weaponsFile);
        if (!yaml.isConfigurationSection("weapons")) {
            return;
        }
        for (String traitlineId : yaml.getConfigurationSection("weapons").getKeys(false)) {
            ItemStack item = readWeapon(yaml, traitlineId);
            if (item != null && item.getType() != Material.AIR) {
                traitlineWeapons.put(traitlineId.toLowerCase(), item.clone());
            }
        }
        Hecate.log("Loaded " + traitlineWeapons.size() + " arena traitline weapons.");
    }

    public void saveWeapon(String traitlineId, ItemStack item) throws IOException {
        ItemStack weapon = item.clone();
        weapon.setAmount(1);
        traitlineWeapons.put(traitlineId.toLowerCase(), weapon.clone());
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(weaponsFile);
        yaml.set("weapons." + traitlineId.toLowerCase(), Base64.getEncoder().encodeToString(weapon.serializeAsBytes()));
        yaml.save(weaponsFile);
    }

    private ItemStack readWeapon(YamlConfiguration yaml, String traitlineId) {
        String path = "weapons." + traitlineId;
        String encoded = yaml.getString(path);
        if (encoded != null && !encoded.isBlank()) {
            try {
                return ItemStack.deserializeBytes(Base64.getDecoder().decode(encoded));
            } catch (Exception e) {
                Hecate.log("Failed to deserialize arena weapon for traitline " + traitlineId + ": " + e.getMessage());
            }
        }
        ItemStack legacy = yaml.getItemStack(path);
        if (legacy != null && legacy.getType() != Material.AIR) {
            return legacy;
        }
        return null;
    }

    public ArenaDefinition createSetup(Player player, String id) {
        ArenaDefinition definition = new ArenaDefinition(id);
        definition.setLobby(ArenaLocation.from(player.getLocation()));
        setupSessions.put(player.getUniqueId(), new ArenaSetupSession(definition));
        return definition;
    }

    public ArenaDefinition editSetup(Player player, String id) {
        ArenaDefinition definition = arenas.get(id.toLowerCase());
        if (definition == null) {
            return null;
        }
        setupSessions.put(player.getUniqueId(), new ArenaSetupSession(definition));
        return definition;
    }

    public ArenaSetupSession getSetup(Player player) {
        return setupSessions.get(player.getUniqueId());
    }

    public void saveSetup(Player player) throws IOException {
        ArenaSetupSession session = getSetup(player);
        if (session == null) {
            return;
        }
        session.definition().save(arenasFolder);
        arenas.put(session.definition().getId(), session.definition());
    }

    public ArenaDefinition findAvailableArena(ArenaMode mode, int teamSize) {
        return arenas.values().stream()
                .filter(arena -> arena.isUsableFor(mode, teamSize))
                .min(Comparator.comparing(ArenaDefinition::getId))
                .orElse(null);
    }

    public void startMatch(ArenaDefinition arena, ArenaQueueType queueType, ArenaQueueEntry first, ArenaQueueEntry second) {
        ArenaMatch match = new ArenaMatch(this, arena, queueType, first, second);
        activeMatches.add(match);
        for (UUID uuid : first.playerIds()) {
            playerMatches.put(uuid, match);
        }
        for (UUID uuid : second.playerIds()) {
            playerMatches.put(uuid, match);
        }
        match.start();
    }

    public void completeMatch(ArenaMatch match, String reason) {
        plugin.getDatabaseManager().recordArenaMatch(match, reason).thenRun(() -> {
            if (match.getQueueType() == ArenaQueueType.RANKED && match.getWinner() >= 0) {
                plugin.getDatabaseManager().updateArenaRatings(match).thenAccept(changes ->
                        Bukkit.getScheduler().runTask(plugin, () -> changes.forEach((uuid, change) -> {
                            Player player = Bukkit.getPlayer(uuid);
                            if (player != null && player.isOnline()) {
                                player.sendMessage(Component.translatable("hecate.arena.rating.changed",
                                        Component.text(String.valueOf(Math.round(change.before()))),
                                        Component.text(String.valueOf(Math.round(change.after()))),
                                        Component.text(formatRatingDelta(change.delta())),
                                        Component.text(String.valueOf(Math.round(change.deviation())))));
                            }
                        })));
            }
        });
    }

    private String formatRatingDelta(double delta) {
        long rounded = Math.round(delta);
        return rounded > 0 ? "+" + rounded : String.valueOf(rounded);
    }

    public void removeMatch(ArenaMatch match) {
        activeMatches.remove(match);
        playerMatches.entrySet().removeIf(entry -> entry.getValue() == match);
    }

    public boolean isInMatch(UUID uuid) {
        return playerMatches.containsKey(uuid);
    }

    public ArenaMatch getMatch(UUID uuid) {
        return playerMatches.get(uuid);
    }

    public int getMaxArenaLevel(HCharacter character) {
        return ARENA_LEVEL;
    }

    public ItemStack getWeapon(HCharacter character) {
        if (character == null || character.getTraitline() == null) {
            return null;
        }
        ItemStack item = traitlineWeapons.get(character.getTraitline().getId().toLowerCase());
        return item == null ? null : item.clone();
    }

    public Collection<ArenaDefinition> getArenas() {
        return arenas.values();
    }

    public ArenaQueueManager getQueueManager() {
        return queueManager;
    }

    public File getArenasFolder() {
        return arenasFolder;
    }

    public File getRecoveryFolder() {
        return recoveryFolder;
    }

    public void shutdown() {
        for (ArenaMatch match : new ArrayList<>(activeMatches)) {
            match.cancel("shutdown");
        }
    }

    @EventHandler
    private void onQuit(PlayerQuitEvent event) {
        queueManager.removeInvalid(event.getPlayer());
        ArenaMatch match = getMatch(event.getPlayer().getUniqueId());
        if (match != null) {
            match.cancel("quit");
        }
    }

    @EventHandler
    private void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player player = event.getPlayer();
            if (player.isOnline() && !isInMatch(player.getUniqueId()) && ArenaPlayerState.restoreRecovery(recoveryFolder, player)) {
                player.sendMessage(Component.translatable("hecate.arena.match.recovered"));
            }
        }, 80L);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onDeath(PlayerDeathEvent event) {
        ArenaMatch match = getMatch(event.getPlayer().getUniqueId());
        if (match != null) {
            event.getDrops().clear();
            event.setKeepInventory(true);
            event.setKeepLevel(true);
            event.setDeathMessage(null);
            match.handleDeath(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onRespawn(PlayerRespawnEvent event) {
        ArenaMatch match = getMatch(event.getPlayer().getUniqueId());
        if (match == null) {
            return;
        }
        if (match.getState() != ArenaMatchState.LIVE && match.getState() != ArenaMatchState.COUNTDOWN && match.getState() != ArenaMatchState.INTERMISSION) {
            return;
        }
        Location spawn = match.getRespawnLocation(event.getPlayer().getUniqueId());
        if (spawn == null) {
            return;
        }
        event.setRespawnLocation(spawn);
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player player = event.getPlayer();
            if (player.isOnline() && isInMatch(player.getUniqueId())) {
                match.respawnAtArenaSpawn(player);
            }
        });
    }

    @EventHandler
    private void onDrop(PlayerDropItemEvent event) {
        if (isInMatch(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player && isInMatch(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onCommand(PlayerCommandPreprocessEvent event) {
        if (!isInMatch(event.getPlayer().getUniqueId())) {
            return;
        }
        String lower = event.getMessage().toLowerCase();
        if (lower.startsWith("/arena") || event.getPlayer().hasPermission("hecate.arena.bypass")) {
            return;
        }
        event.setCancelled(true);
        event.getPlayer().sendMessage(Component.translatable("hecate.arena.match.command_blocked"));
    }
}
