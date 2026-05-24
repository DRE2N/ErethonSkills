package de.erethon.hecate.arenas;

import de.erethon.hecate.Hecate;
import de.erethon.hecate.data.HCharacter;
import de.erethon.hecate.data.HPlayer;
import de.erethon.hecate.events.CombatModeReason;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellEffect;
import de.erethon.spellbook.teams.SpellbookTeam;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class ArenaMatch {

    private final UUID id = UUID.randomUUID();
    private final ArenaManager manager;
    private final ArenaDefinition arena;
    private final ArenaQueueType queueType;
    private final List<ArenaTeamEntry> teams = new ArrayList<>();
    private final Map<UUID, ArenaPlayerState> states = new HashMap<>();
    private final Map<String, ControlPointState> controlPoints = new LinkedHashMap<>();
    private final Map<String, String> controlPointNames = new LinkedHashMap<>();
    private final Map<Location, BlockData> controlMarkerOriginalBlocks = new HashMap<>();
    private final int[] scores = new int[]{0, 0};
    private final List<EscortRoundResult> escortResults = new ArrayList<>();
    private ArenaMatchState state = ArenaMatchState.PREPARING;
    private BukkitTask task;
    private int elapsedTicks;
    private int countdownTicks = 100;
    private int escortRound = 0;
    private double payloadProgress;
    private int winner = -1;
    private BlockDisplay payloadDisplay;
    private BossBar objectiveBar;

    public ArenaMatch(ArenaManager manager, ArenaDefinition arena, ArenaQueueType queueType, ArenaQueueEntry first, ArenaQueueEntry second) {
        this.manager = manager;
        this.arena = arena;
        this.queueType = queueType;
        teams.add(new ArenaTeamEntry(0, first.playerIds()));
        teams.add(new ArenaTeamEntry(1, second.playerIds()));
        int pointIndex = 0;
        for (CapturePointDefinition point : arena.getCapturePoints()) {
            if (point.location() == null || point.location().toBukkit() == null) {
                continue;
            }
            controlPoints.put(point.id(), new ControlPointState(point));
            controlPointNames.put(point.id(), pointName(pointIndex++));
        }
    }

    public void start() {
        state = ArenaMatchState.COUNTDOWN;
        forEachPlayer(player -> player.sendMessage(Component.translatable("hecate.arena.match.found", Component.text(arena.getDisplayName()))));
        preparePlayers();
        if (state == ArenaMatchState.COMPLETE || state == ArenaMatchState.CANCELLED) {
            return;
        }
        createObjectiveBar();
        task = new BukkitRunnable() {
            @Override
            public void run() {
                tick();
            }
        }.runTaskTimer(Hecate.getInstance(), 1L, 1L);
    }

    private void preparePlayers() {
        for (ArenaTeamEntry team : teams) {
            Location spawn = arena.getTeamSpawns().get(team.getId()).toBukkit();
            if (spawn == null) {
                cancel("missing world");
                return;
            }
            for (UUID uuid : team.getPlayers()) {
                Player player = Bukkit.getPlayer(uuid);
                if (player == null) {
                    cancel("offline");
                    return;
                }
                HPlayer hPlayer = Hecate.getInstance().getDatabaseManager().getHPlayer(player);
                HCharacter character = hPlayer == null ? null : hPlayer.getSelectedCharacter();
                if (character == null) {
                    cancel("missing character");
                    return;
                }
                character.saveCharacterPlayerData(false).join();
                ArenaPlayerState playerState = new ArenaPlayerState(player, character);
                states.put(uuid, playerState);
                playerState.saveRecovery(manager.getRecoveryFolder(), uuid);
                assignSpellbookTeam(player, team.getId());
                character.setSaveInventory(false);
                player.teleport(spawn);
                player.setGameMode(GameMode.ADVENTURE);
                player.setFoodLevel(20);
                player.setSaturation(20);
                clearSpellEffects(player);
                ItemStack weapon = manager.getWeapon(character);
                player.getInventory().clear();
                player.getInventory().setArmorContents(new ItemStack[4]);
                player.getInventory().setItemInOffHand(null);
                player.getInventory().setHeldItemSlot(8);
                if (weapon != null) {
                    player.getInventory().setItem(8, weapon);
                }
                character.switchCastMode(CombatModeReason.ARENA, true);
                Bukkit.getScheduler().runTaskLater(Hecate.getInstance(), () -> {
                    character.setScaledPvP(true, manager.getMaxArenaLevel(character));
                    Bukkit.getScheduler().runTask(Hecate.getInstance(), () -> healToFull(player));
                }, 10L);
            }
        }
    }

    private void healToFull(Player player) {
        if (!player.isOnline() || player.isDead()) {
            return;
        }
        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        player.setHealth(maxHealth == null ? player.getMaxHealth() : maxHealth.getValue());
        player.setFoodLevel(20);
        player.setSaturation(20);
    }

    private void tick() {
        enforceArenaMode();
        if (state == ArenaMatchState.COUNTDOWN) {
            countdownTicks--;
            if (countdownTicks <= 0) {
                state = ArenaMatchState.LIVE;
                elapsedTicks = 0;
                forEachPlayer(player -> player.sendMessage(Component.translatable("hecate.arena.match.started")));
            }
            updateObjectiveBar();
            return;
        }
        if (state != ArenaMatchState.LIVE) {
            return;
        }
        elapsedTicks++;
        if (arena.getMode() == ArenaMode.CONTROL) {
            tickControl();
        } else {
            tickEscort();
        }
        updateObjectiveBar();
    }

    private void tickControl() {
        for (ControlPointState point : controlPoints.values()) {
            Location location = point.getDefinition().location().toBukkit();
            if (location == null) {
                continue;
            }
            int contestingTeam = getContestingTeam(location, point.getDefinition().radius());
            point.contest(contestingTeam);
            if (point.getOwner() >= 0 && elapsedTicks % 20 == 0) {
                scores[point.getOwner()] += point.getDefinition().scorePerSecond();
            }
            if (elapsedTicks % 10 == 0) {
                renderControlMarker(point, location);
                Color color = switch (point.getOwner()) {
                    case 0 -> Color.RED;
                    case 1 -> Color.BLUE;
                    default -> Color.GRAY;
                };
                location.getWorld().spawnParticle(Particle.DUST, location.clone().add(0, 1, 0), 8, point.getDefinition().radius() / 2, 0.2, point.getDefinition().radius() / 2, new Particle.DustOptions(color, 1.2f));
            }
        }
        if (scores[0] >= arena.getControlScoreLimit() || scores[1] >= arena.getControlScoreLimit() || elapsedTicks >= arena.getControlTimeSeconds() * 20) {
            finish(scores[0] == scores[1] ? -1 : (scores[0] > scores[1] ? 0 : 1), "score");
        }
    }

    private void renderControlMarker(ControlPointState point, Location location) {
        if (location.getWorld() == null) {
            return;
        }
        int radius = Math.max(1, (int) Math.ceil(point.getDefinition().radius()));
        List<Block> blocks = new ArrayList<>();
        Block center = location.getBlock().getRelative(0, -1, 0);
        int radiusSquared = radius * radius;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x * x + z * z > radiusSquared) {
                    continue;
                }
                blocks.add(center.getRelative(x, 0, z));
            }
        }
        blocks.sort((first, second) -> Double.compare(first.getLocation().distanceSquared(center.getLocation()), second.getLocation().distanceSquared(center.getLocation())));
        int progressBlocks = Math.round(blocks.size() * point.getProgress());
        Material base = controlMaterial(point.getOwner());
        Material progress = controlMaterial(point.getProgressTeam());
        for (int i = 0; i < blocks.size(); i++) {
            Block block = blocks.get(i);
            Location key = block.getLocation();
            controlMarkerOriginalBlocks.putIfAbsent(key, block.getBlockData().clone());
            BlockData displayData = (i < progressBlocks && point.getProgressTeam() >= 0 ? progress : base).createBlockData();
            forEachPlayer(player -> player.sendBlockChange(key, displayData));
        }
    }

    private Material controlMaterial(int team) {
        return switch (team) {
            case 0 -> Material.RED_CONCRETE;
            case 1 -> Material.BLUE_CONCRETE;
            default -> Material.WHITE_CONCRETE;
        };
    }

    private void tickEscort() {
        PayloadDefinition payload = arena.getPayload();
        if (payload.getNodes().size() < 2) {
            finish(-1, "invalid payload");
            return;
        }
        int attackingTeam = escortRound == 0 ? 0 : 1;
        int defendingTeam = 1 - attackingTeam;
        int roundTicks = payload.getRoundSeconds() * 20;
        Location payloadLocation = getPayloadLocation(payloadProgress);
        updatePayloadDisplay(payloadLocation);
        int attackers = countTeamNear(attackingTeam, payloadLocation, payload.getRadius());
        int defenders = countTeamNear(defendingTeam, payloadLocation, payload.getRadius());
        if (attackers > 0 && defenders == 0) {
            payloadProgress += payload.getBlocksPerSecond() / 20.0;
        }
        if (elapsedTicks % 10 == 0 && payloadLocation != null) {
            payloadLocation.getWorld().spawnParticle(Particle.DUST, payloadLocation.clone().add(0, 1, 0), 10, 0.6, 0.3, 0.6, new Particle.DustOptions(Color.YELLOW, 1.4f));
        }
        boolean completed = payloadProgress >= getPayloadLength();
        if (completed || elapsedTicks >= roundTicks) {
            escortResults.add(new EscortRoundResult(attackingTeam, Math.min(payloadProgress, getPayloadLength()), elapsedTicks / 20, completed));
            if (escortRound == 0) {
                startSecondEscortRound();
            } else {
                finish(compareEscortResults(), "escort");
            }
        }
    }

    private void startSecondEscortRound() {
        state = ArenaMatchState.INTERMISSION;
        escortRound = 1;
        payloadProgress = 0;
        elapsedTicks = 0;
        removePayloadDisplay();
        forEachPlayer(player -> player.sendMessage(Component.translatable("hecate.arena.escort.round_swap")));
        Bukkit.getScheduler().runTaskLater(Hecate.getInstance(), () -> {
            for (ArenaTeamEntry team : teams) {
                Location spawn = arena.getTeamSpawns().get(team.getId()).toBukkit();
                for (UUID uuid : team.getPlayers()) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null) {
                        player.teleport(spawn);
                        healToFull(player);
                    }
                }
            }
            state = ArenaMatchState.LIVE;
            updateObjectiveBar();
        }, 100L);
    }

    private void createObjectiveBar() {
        objectiveBar = BossBar.bossBar(Component.empty(), 0.0f, BossBar.Color.BLUE, BossBar.Overlay.PROGRESS);
        updateObjectiveBar();
        forEachPlayer(player -> player.showBossBar(objectiveBar));
    }

    private void updateObjectiveBar() {
        if (objectiveBar == null) {
            return;
        }
        if (state == ArenaMatchState.COUNTDOWN) {
            objectiveBar.name(Component.translatable("hecate.arena.match.countdown", Component.text(Math.max(0, countdownTicks / 20))));
            objectiveBar.progress(Math.max(0.0f, Math.min(1.0f, countdownTicks / 100.0f)));
            objectiveBar.color(BossBar.Color.YELLOW);
            return;
        }
        if (arena.getMode() == ArenaMode.CONTROL) {
            updateControlBossBar();
        } else {
            updateEscortBossBar();
        }
    }

    private void updateControlBossBar() {
        int timeLeft = Math.max(0, arena.getControlTimeSeconds() - (elapsedTicks / 20));
        Component status = Component.text("Red ", NamedTextColor.RED)
                .append(Component.text(scores[0] + "/" + arena.getControlScoreLimit(), NamedTextColor.WHITE))
                .append(Component.text("  Blue ", NamedTextColor.BLUE))
                .append(Component.text(scores[1] + "/" + arena.getControlScoreLimit(), NamedTextColor.WHITE))
                .append(Component.text("  " + formatTime(timeLeft) + "  ", NamedTextColor.GRAY));
        for (ControlPointState point : controlPoints.values()) {
            status = status.append(Component.text(controlPointNames.get(point.getDefinition().id()) + ":", NamedTextColor.YELLOW))
                    .append(Component.text(ownerLabel(point.getOwner()) + " ", ownerColor(point.getOwner())));
        }
        objectiveBar.name(status);
        objectiveBar.progress(Math.max(0.0f, Math.min(1.0f, Math.max(scores[0], scores[1]) / (float) arena.getControlScoreLimit())));
        objectiveBar.color(scores[0] == scores[1] ? BossBar.Color.WHITE : (scores[0] > scores[1] ? BossBar.Color.RED : BossBar.Color.BLUE));
    }

    private void updateEscortBossBar() {
        PayloadDefinition payload = arena.getPayload();
        double length = Math.max(1.0, getPayloadLength());
        int timeLeft = Math.max(0, payload.getRoundSeconds() - (elapsedTicks / 20));
        int attackingTeam = escortRound == 0 ? 0 : 1;
        Component status = Component.text("Payload ", NamedTextColor.YELLOW)
                .append(Component.text((int) Math.min(payloadProgress, length) + "/" + (int) length + " blocks", NamedTextColor.WHITE))
                .append(Component.text("  Round " + (escortRound + 1), NamedTextColor.GRAY))
                .append(Component.text("  Attack: ", NamedTextColor.GRAY))
                .append(Component.text(attackingTeam == 0 ? "Red" : "Blue", attackingTeam == 0 ? NamedTextColor.RED : NamedTextColor.BLUE))
                .append(Component.text("  " + formatTime(timeLeft), NamedTextColor.GRAY));
        objectiveBar.name(status);
        objectiveBar.progress(Math.max(0.0f, Math.min(1.0f, (float) (payloadProgress / length))));
        objectiveBar.color(attackingTeam == 0 ? BossBar.Color.RED : BossBar.Color.BLUE);
    }

    private String pointName(int index) {
        StringBuilder name = new StringBuilder();
        int value = index;
        do {
            name.insert(0, (char) ('A' + (value % 26)));
            value = value / 26 - 1;
        } while (value >= 0);
        return name.toString();
    }

    private String ownerLabel(int owner) {
        return switch (owner) {
            case 0 -> "R";
            case 1 -> "B";
            default -> "N";
        };
    }

    private NamedTextColor ownerColor(int owner) {
        return switch (owner) {
            case 0 -> NamedTextColor.RED;
            case 1 -> NamedTextColor.BLUE;
            default -> NamedTextColor.GRAY;
        };
    }

    private String formatTime(int seconds) {
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }

    private void enforceArenaMode() {
        forEachPlayer(player -> {
            if (player.getGameMode() != GameMode.ADVENTURE) {
                player.setGameMode(GameMode.ADVENTURE);
            }
        });
    }

    private void updatePayloadDisplay(Location payloadLocation) {
        if (arena.getMode() != ArenaMode.ESCORT || payloadLocation == null) {
            return;
        }
        Location displayLocation = payloadLocation.clone().add(0, 0.3, 0);
        // Zero out pitch
        displayLocation.setRotation(displayLocation.getYaw(), 0);
        if (payloadDisplay == null || !payloadDisplay.isValid()) {
            payloadDisplay = payloadLocation.getWorld().spawn(displayLocation, BlockDisplay.class, display -> {
                display.setPersistent(false);
                display.setGlowing(true);
                display.setTeleportDuration(2);
                display.setBlock(Material.COPPER_BLOCK.createBlockData());
                display.setTransformation(new Transformation(
                        new Vector3f(-1.0f, 0.0f, -1.0f),
                        new AxisAngle4f(),
                        new Vector3f(2.0f, 2.0f, 2.0f),
                        new AxisAngle4f()
                ));
            });
        } else {
            payloadDisplay.teleport(displayLocation);
        }
    }

    private void removePayloadDisplay() {
        if (payloadDisplay != null && payloadDisplay.isValid()) {
            payloadDisplay.remove();
        }
        payloadDisplay = null;
    }

    private int compareEscortResults() {
        EscortRoundResult first = escortResults.get(0);
        EscortRoundResult second = escortResults.get(1);
        if (first.completed() && second.completed()) {
            return first.elapsedSeconds() == second.elapsedSeconds() ? -1 : (first.elapsedSeconds() < second.elapsedSeconds() ? first.attackingTeam() : second.attackingTeam());
        }
        if (first.completed() != second.completed()) {
            return first.completed() ? first.attackingTeam() : second.attackingTeam();
        }
        return Double.compare(first.progress(), second.progress()) == 0 ? -1 : (first.progress() > second.progress() ? first.attackingTeam() : second.attackingTeam());
    }

    private int getContestingTeam(Location location, double radius) {
        int t0 = countTeamNear(0, location, radius);
        int t1 = countTeamNear(1, location, radius);
        if (t0 == t1) {
            return -1;
        }
        return t0 > t1 ? 0 : 1;
    }

    private int countTeamNear(int teamId, Location location, double radius) {
        if (location == null) {
            return 0;
        }
        int count = 0;
        double radiusSquared = radius * radius;
        for (UUID uuid : teams.get(teamId).getPlayers()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.getWorld().equals(location.getWorld()) && player.getLocation().distanceSquared(location) <= radiusSquared && !player.isDead()) {
                count++;
            }
        }
        return count;
    }

    private Location getPayloadLocation(double distance) {
        PayloadDefinition payload = arena.getPayload();
        double remaining = distance;
        for (int i = 0; i < payload.getNodes().size() - 1; i++) {
            Location a = payload.getNodes().get(i).toBukkit();
            Location b = payload.getNodes().get(i + 1).toBukkit();
            if (a == null || b == null) {
                return null;
            }
            double segment = a.distance(b);
            if (remaining <= segment) {
                Vector direction = b.toVector().subtract(a.toVector()).normalize().multiply(remaining);
                return a.clone().add(direction);
            }
            remaining -= segment;
        }
        return payload.getNodes().get(payload.getNodes().size() - 1).toBukkit();
    }

    private double getPayloadLength() {
        double length = 0;
        List<ArenaLocation> nodes = arena.getPayload().getNodes();
        for (int i = 0; i < nodes.size() - 1; i++) {
            Location a = nodes.get(i).toBukkit();
            Location b = nodes.get(i + 1).toBukkit();
            if (a != null && b != null) {
                length += a.distance(b);
            }
        }
        return length;
    }

    public void handleDeath(Player player) {
        awardControlKillScore(player);
        clearSpellEffects(player);
        Bukkit.getScheduler().runTask(Hecate.getInstance(), () -> clearSpellEffects(player));
        Bukkit.getScheduler().runTaskLater(Hecate.getInstance(), () -> {
            if (state == ArenaMatchState.LIVE && player.isOnline()) {
                clearSpellEffects(player);
                respawnAtArenaSpawn(player);
            }
        }, 40L);
    }

    private void awardControlKillScore(Player victim) {
        if (state != ArenaMatchState.LIVE || arena.getMode() != ArenaMode.CONTROL || arena.getControlPointsPerKill() <= 0) {
            return;
        }
        Player killer = victim.getKiller();
        if (killer == null || killer.getUniqueId().equals(victim.getUniqueId())) {
            return;
        }
        ArenaTeamEntry killerTeam = getTeam(killer.getUniqueId());
        ArenaTeamEntry victimTeam = getTeam(victim.getUniqueId());
        if (killerTeam == null || victimTeam == null || killerTeam.getId() == victimTeam.getId()) {
            return;
        }
        scores[killerTeam.getId()] += arena.getControlPointsPerKill();
        updateObjectiveBar();
        if (scores[killerTeam.getId()] >= arena.getControlScoreLimit()) {
            finish(killerTeam.getId(), "score");
        }
    }

    private void assignSpellbookTeam(Player player, int teamId) {
        String id = "hecate_arena_" + this.id + "_" + teamId;
        SpellbookTeam team = Spellbook.getInstance().getTeamManager().getTeam(id);
        if (team == null) {
            Spellbook.getInstance().getTeamManager().createTeam(id, teamId == 0 ? "Arena Red" : "Arena Blue", teamId == 0 ? Color.RED : Color.BLUE);
            team = Spellbook.getInstance().getTeamManager().getTeam(id);
        }
        if (team != null) {
            Spellbook.getInstance().getTeamManager().addEntityToTeam(player, team, false);
        }
    }

    public Location getRespawnLocation(UUID uuid) {
        ArenaTeamEntry team = getTeam(uuid);
        if (team == null) {
            return null;
        }
        ArenaLocation location = arena.getTeamSpawns().get(team.getId());
        return location == null ? null : location.toBukkit();
    }

    public void respawnAtArenaSpawn(Player player) {
        Location spawn = getRespawnLocation(player.getUniqueId());
        if (spawn == null) {
            return;
        }
        clearSpellEffects(player);
        player.teleport(spawn);
        player.setGameMode(GameMode.ADVENTURE);
        player.setFoodLevel(20);
        player.setSaturation(20);
        healToFull(player);
    }

    private void clearSpellEffects(Player player) {
        for (SpellEffect effect : new ArrayList<>(player.getEffects())) {
            try {
                player.removeEffect(effect.data);
            } catch (Exception ignored) {
                effect.onRemove();
            }
        }
        for (SpellEffect effect : new ArrayList<>(player.getEffects())) {
            effect.onRemove();
        }
        player.getEffects().clear();
    }

    public void cancel(String reason) {
        if (state == ArenaMatchState.COMPLETE) {
            return;
        }
        state = ArenaMatchState.CANCELLED;
        finish(-1, reason);
    }

    private void finish(int winner, String reason) {
        if (state == ArenaMatchState.COMPLETE) {
            return;
        }
        this.winner = winner;
        state = ArenaMatchState.COMPLETE;
        if (task != null) {
            task.cancel();
        }
        hideObjectiveBar();
        removePayloadDisplay();
        forEachPlayer(player -> {
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.1f);
            if (winner == -1) {
                player.sendMessage(Component.translatable("hecate.arena.match.draw"));
            } else if (getTeam(player.getUniqueId()).getId() == winner) {
                player.sendMessage(Component.translatable("hecate.arena.match.win"));
            } else {
                player.sendMessage(Component.translatable("hecate.arena.match.loss"));
            }
        });
        manager.completeMatch(this, reason);
        cleanup();
    }

    private void cleanup() {
        hideObjectiveBar();
        restoreControlMarkers();
        for (Map.Entry<UUID, ArenaPlayerState> entry : states.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            ArenaPlayerState state = entry.getValue();
            HCharacter character = state.getCharacter();
            character.setScaledPvP(false);
            if (character.isInCastMode() && !state.wasInCastMode()) {
                character.switchCastMode(CombatModeReason.ARENA, false);
            }
            if (player != null && player.isOnline()) {
                Spellbook.getInstance().getTeamManager().setTeam(player, state.getSpellbookTeam());
                state.restore(player);
            }
            ArenaPlayerState.clearRecovery(manager.getRecoveryFolder(), entry.getKey());
            character.setSaveInventory(true);
        }
        removeSpellbookArenaTeams();
        manager.removeMatch(this);
    }

    private void removeSpellbookArenaTeams() {
        for (ArenaTeamEntry team : teams) {
            Spellbook.getInstance().getTeamManager().removeTeam("hecate_arena_" + id + "_" + team.getId());
        }
    }

    private void restoreControlMarkers() {
        for (Map.Entry<Location, BlockData> entry : controlMarkerOriginalBlocks.entrySet()) {
            Location location = entry.getKey();
            if (location.getWorld() != null) {
                forEachPlayer(player -> player.sendBlockChange(location, entry.getValue()));
            }
        }
        controlMarkerOriginalBlocks.clear();
    }

    private void hideObjectiveBar() {
        if (objectiveBar == null) {
            return;
        }
        forEachPlayer(player -> player.hideBossBar(objectiveBar));
        objectiveBar = null;
    }

    private void forEachPlayer(Consumer<Player> consumer) {
        for (ArenaTeamEntry team : teams) {
            for (UUID uuid : team.getPlayers()) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    consumer.accept(player);
                }
            }
        }
    }

    public ArenaTeamEntry getTeam(UUID uuid) {
        for (ArenaTeamEntry team : teams) {
            if (team.contains(uuid)) {
                return team;
            }
        }
        return null;
    }

    public UUID getId() { return id; }
    public ArenaDefinition getArena() { return arena; }
    public ArenaQueueType getQueueType() { return queueType; }
    public List<ArenaTeamEntry> getTeams() { return teams; }
    public int getWinner() { return winner; }
    public ArenaMatchState getState() { return state; }
}
