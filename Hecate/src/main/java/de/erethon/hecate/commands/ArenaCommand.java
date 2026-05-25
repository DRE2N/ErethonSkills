package de.erethon.hecate.commands;

import de.erethon.bedrock.command.ECommand;
import de.erethon.hecate.Hecate;
import de.erethon.hecate.arenas.ArenaDefinition;
import de.erethon.hecate.arenas.ArenaLeaderboardEntry;
import de.erethon.hecate.arenas.ArenaLocation;
import de.erethon.hecate.arenas.ArenaMode;
import de.erethon.hecate.arenas.ArenaQueueType;
import de.erethon.hecate.arenas.ArenaSetupSession;
import de.erethon.hecate.classes.Traitline;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ArenaCommand extends ECommand implements TabCompleter {

    public ArenaCommand() {
        setCommand("arena");
        setMinArgs(0);
        setMaxArgs(8);
        setPlayerCommand(true);
        setConsoleCommand(false);
        setRegisterSeparately(true);
        setHelp("/arena join <ranked|unranked> <team-size> <control|escort|any>");
    }

    @Override
    public void onExecute(String[] args, CommandSender commandSender) {
        Player player = (Player) commandSender;
        if (args.length <= 1 || args[1].equalsIgnoreCase("help")) {
            showHelp(player);
            return;
        }
        switch (args[1].toLowerCase()) {
            case "join" -> join(player, args);
            case "leave" -> leave(player);
            case "status" -> status(player);
            case "rating" -> rating(player);
            case "top" -> top(player, args);
            case "create" -> create(player, args);
            case "edit" -> edit(player, args);
            case "setmode" -> setMode(player, args);
            case "setcontrol" -> setControl(player, args);
            case "setspawn" -> setSpawn(player, args);
            case "addpoint" -> addPoint(player, args);
            case "payload" -> payload(player, args);
            case "setlobby" -> setLobby(player);
            case "save" -> save(player);
            case "list" -> list(player);
            case "enable" -> enable(player, args);
            case "weapon" -> weapon(player, args);
            default -> player.sendMessage(Component.translatable("hecate.arena.command.unknown"));
        }
    }

    private void showHelp(Player player) {
        player.sendMessage(Component.translatable("hecate.arena.command.help_title"));
        player.sendMessage(Component.translatable("hecate.arena.command.help_join"));
        player.sendMessage(Component.translatable("hecate.arena.command.help_leave"));
        player.sendMessage(Component.translatable("hecate.arena.command.help_top"));
        if (player.hasPermission("hecate.arena.admin")) {
            player.sendMessage(Component.translatable("hecate.arena.command.help_admin"));
        }
    }

    private void join(Player player, String[] args) {
        if (args.length < 5) {
            player.sendMessage(Component.translatable("hecate.arena.command.join_usage"));
            return;
        }
        ArenaQueueType type = ArenaQueueType.parse(args[2]);
        int teamSize = parseTeamSize(args[3]);
        ArenaMode mode = args[4].equalsIgnoreCase("any") ? null : ArenaMode.parse(args[4]);
        if (type == null || teamSize < 1 || (!args[4].equalsIgnoreCase("any") && mode == null)) {
            player.sendMessage(Component.translatable("hecate.arena.command.join_usage"));
            return;
        }
        Hecate.getInstance().getArenaManager().getQueueManager().join(player, type, teamSize, mode);
    }

    private int parseTeamSize(String input) {
        try {
            if (input.contains("v")) {
                String[] split = input.toLowerCase().split("v");
                int left = Integer.parseInt(split[0]);
                int right = Integer.parseInt(split[1]);
                return left == right ? left : -1;
            }
            return Integer.parseInt(input);
        } catch (Exception ignored) {
            return -1;
        }
    }

    private void leave(Player player) {
        if (Hecate.getInstance().getArenaManager().isInMatch(player.getUniqueId())) {
            Hecate.getInstance().getArenaManager().getMatch(player.getUniqueId()).cancel("leave");
            return;
        }
        Hecate.getInstance().getArenaManager().getQueueManager().leave(player);
    }

    private void status(Player player) {
        if (Hecate.getInstance().getArenaManager().isInMatch(player.getUniqueId())) {
            player.sendMessage(Component.translatable("hecate.arena.status.in_match"));
            return;
        }
        String queue = Hecate.getInstance().getArenaManager().getQueueManager().getStatus(player.getUniqueId());
        if (queue == null) {
            player.sendMessage(Component.translatable("hecate.arena.status.not_queued"));
        } else {
            player.sendMessage(Component.translatable("hecate.arena.status.queued", Component.text(queue)));
        }
    }

    private void rating(Player player) {
        Hecate.getInstance().getDatabaseManager().getArenaRating(player.getUniqueId()).thenAccept(rating -> {
            player.sendMessage(Component.translatable("hecate.arena.rating.current",
                    Component.text(String.valueOf(Math.round(rating.getRating()))),
                    Component.text(String.valueOf(Math.round(rating.getDeviation())))));
        });
    }

    private void top(Player player, String[] args) {
        int limit = 10;
        if (args.length >= 3) {
            try {
                limit = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                player.sendMessage(Component.translatable("hecate.arena.command.top_usage"));
                return;
            }
        }
        if (limit < 1 || limit > 100) {
            player.sendMessage(Component.translatable("hecate.arena.command.top_usage"));
            return;
        }
        int finalLimit = limit;
        Hecate.getInstance().getDatabaseManager().getTopArenaRatings(finalLimit).thenAccept(entries ->
                Bukkit.getScheduler().runTask(Hecate.getInstance(), () -> sendTop(player, entries)));
    }

    private void sendTop(Player player, List<ArenaLeaderboardEntry> entries) {
        player.sendMessage(Component.translatable("hecate.arena.rating.top_title"));
        if (entries.isEmpty()) {
            player.sendMessage(Component.translatable("hecate.arena.rating.top_empty"));
            return;
        }
        int rank = 1;
        for (ArenaLeaderboardEntry entry : entries) {
            String name = entry.playerName();
            if (name == null || name.isBlank()) {
                name = entry.playerId().toString();
            }
            player.sendMessage(Component.translatable("hecate.arena.rating.top_entry",
                    Component.text(rank++),
                    Component.text(name),
                    Component.text(String.valueOf(Math.round(entry.rating()))),
                    Component.text(String.valueOf(Math.round(entry.deviation())))));
        }
    }

    private boolean requireAdmin(Player player) {
        if (!player.hasPermission("hecate.arena.admin")) {
            player.sendMessage(Component.translatable("hecate.general.permission_denied"));
            return false;
        }
        return true;
    }

    private ArenaSetupSession requireSetup(Player player) {
        ArenaSetupSession session = Hecate.getInstance().getArenaManager().getSetup(player);
        if (session == null) {
            player.sendMessage(Component.translatable("hecate.arena.setup.no_session"));
        }
        return session;
    }

    private void create(Player player, String[] args) {
        if (!requireAdmin(player)) return;
        if (args.length < 3) {
            player.sendMessage(Component.translatable("hecate.arena.setup.create_usage"));
            return;
        }
        ArenaDefinition definition = Hecate.getInstance().getArenaManager().createSetup(player, args[2]);
        player.sendMessage(Component.translatable("hecate.arena.setup.created", Component.text(definition.getId())));
    }

    private void edit(Player player, String[] args) {
        if (!requireAdmin(player)) return;
        if (args.length < 3) {
            player.sendMessage(Component.translatable("hecate.arena.setup.edit_usage"));
            return;
        }
        ArenaDefinition definition = Hecate.getInstance().getArenaManager().editSetup(player, args[2]);
        if (definition == null) {
            player.sendMessage(Component.translatable("hecate.arena.setup.not_found", Component.text(args[2])));
            return;
        }
        player.sendMessage(Component.translatable("hecate.arena.setup.editing", Component.text(definition.getId())));
    }

    private void setMode(Player player, String[] args) {
        if (!requireAdmin(player)) return;
        ArenaSetupSession session = requireSetup(player);
        if (session == null) return;
        if (args.length < 4) {
            player.sendMessage(Component.translatable("hecate.arena.setup.setmode_usage"));
            return;
        }
        ArenaMode mode = ArenaMode.parse(args[2]);
        int teamSize = parseTeamSize(args[3]);
        if (mode == null || teamSize < 1 || teamSize > 5) {
            player.sendMessage(Component.translatable("hecate.arena.setup.setmode_usage"));
            return;
        }
        session.definition().setMode(mode);
        session.definition().setTeamSize(teamSize);
        player.sendMessage(Component.translatable("hecate.arena.setup.mode_set", Component.text(mode.name()), Component.text(teamSize + "v" + teamSize)));
    }

    private void setControl(Player player, String[] args) {
        if (!requireAdmin(player)) return;
        ArenaSetupSession session = requireSetup(player);
        if (session == null) return;
        if (args.length < 4) {
            player.sendMessage(Component.translatable("hecate.arena.setup.setcontrol_usage"));
            return;
        }
        int scoreLimit;
        int timeSeconds;
        int pointsPerKill = session.definition().getControlPointsPerKill();
        try {
            scoreLimit = Integer.parseInt(args[2]);
            timeSeconds = Integer.parseInt(args[3]);
            if (args.length >= 5) {
                pointsPerKill = Integer.parseInt(args[4]);
            }
        } catch (NumberFormatException e) {
            player.sendMessage(Component.translatable("hecate.arena.setup.setcontrol_usage"));
            return;
        }
        if (scoreLimit < 1 || timeSeconds < 1 || pointsPerKill < 0) {
            player.sendMessage(Component.translatable("hecate.arena.setup.setcontrol_usage"));
            return;
        }
        session.definition().setControlScoreLimit(scoreLimit);
        session.definition().setControlTimeSeconds(timeSeconds);
        session.definition().setControlPointsPerKill(pointsPerKill);
        player.sendMessage(Component.translatable("hecate.arena.setup.control_set", Component.text(scoreLimit), Component.text(timeSeconds), Component.text(pointsPerKill)));
    }

    private void setSpawn(Player player, String[] args) {
        if (!requireAdmin(player)) return;
        ArenaSetupSession session = requireSetup(player);
        if (session == null) return;
        if (args.length < 3) {
            player.sendMessage(Component.translatable("hecate.arena.setup.setspawn_usage"));
            return;
        }
        int team;
        try {
            team = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            player.sendMessage(Component.translatable("hecate.arena.setup.setspawn_usage"));
            return;
        }
        if (team < 0 || team > 1) {
            player.sendMessage(Component.translatable("hecate.arena.setup.setspawn_usage"));
            return;
        }
        session.definition().getTeamSpawns().put(team, ArenaLocation.from(player.getLocation()));
        player.sendMessage(Component.translatable("hecate.arena.setup.spawn_set", Component.text(team)));
    }

    private void addPoint(Player player, String[] args) {
        if (!requireAdmin(player)) return;
        ArenaSetupSession session = requireSetup(player);
        if (session == null) return;
        String id = args.length >= 3 ? args[2] : nextPointName(session.definition());
        session.definition().getCapturePointMap().put(id, new de.erethon.hecate.arenas.CapturePointDefinition(id, ArenaLocation.from(player.getLocation()), 5.0, 1, 8, 5));
        player.sendMessage(Component.translatable("hecate.arena.setup.point_added", Component.text(id)));
    }

    private String nextPointName(ArenaDefinition definition) {
        int index = 0;
        String name;
        do {
            name = pointName(index++);
        } while (definition.getCapturePointMap().containsKey(name));
        return name;
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

    private void payload(Player player, String[] args) {
        if (!requireAdmin(player)) return;
        ArenaSetupSession session = requireSetup(player);
        if (session == null) return;
        if (args.length >= 3 && args[2].equalsIgnoreCase("addnode")) {
            session.definition().getPayload().getNodes().add(ArenaLocation.from(player.getLocation()));
            player.sendMessage(Component.translatable("hecate.arena.setup.payload_node_added", Component.text(session.definition().getPayload().getNodes().size())));
        } else {
            player.sendMessage(Component.translatable("hecate.arena.setup.payload_usage"));
        }
    }

    private void setLobby(Player player) {
        if (!requireAdmin(player)) return;
        ArenaSetupSession session = requireSetup(player);
        if (session == null) return;
        session.definition().setLobby(ArenaLocation.from(player.getLocation()));
        player.sendMessage(Component.translatable("hecate.arena.setup.lobby_set"));
    }

    private void save(Player player) {
        if (!requireAdmin(player)) return;
        ArenaSetupSession session = requireSetup(player);
        if (session == null) return;
        String validation = session.definition().validate();
        if (!validation.isEmpty()) {
            player.sendMessage(Component.translatable("hecate.arena.setup.validation_failed", Component.text(validation)));
            return;
        }
        try {
            Hecate.getInstance().getArenaManager().saveSetup(player);
            player.sendMessage(Component.translatable("hecate.arena.setup.saved", Component.text(session.definition().getId())));
        } catch (IOException e) {
            player.sendMessage(Component.translatable("hecate.data.error_saving", Component.text(e.getMessage())));
        }
    }

    private void list(Player player) {
        player.sendMessage(Component.translatable("hecate.arena.list.title"));
        for (ArenaDefinition arena : Hecate.getInstance().getArenaManager().getArenas()) {
            player.sendMessage(Component.text("- ", NamedTextColor.GRAY)
                    .append(Component.text(arena.getId(), arena.isEnabled() ? NamedTextColor.GREEN : NamedTextColor.RED))
                    .append(Component.text(" " + arena.getMode() + " " + arena.getTeamSize() + "v" + arena.getTeamSize()
                            + " control=" + arena.getControlScoreLimit() + "/" + arena.getControlTimeSeconds() + "s/" + arena.getControlPointsPerKill() + "pk "
                            + arena.validate(), NamedTextColor.GRAY)));
        }
    }

    private void enable(Player player, String[] args) {
        if (!requireAdmin(player)) return;
        ArenaSetupSession session = requireSetup(player);
        if (session == null) return;
        boolean enabled = args.length < 3 || Boolean.parseBoolean(args[2]);
        session.definition().setEnabled(enabled);
        player.sendMessage(Component.translatable(enabled ? "hecate.arena.setup.enabled" : "hecate.arena.setup.disabled"));
    }

    private void weapon(Player player, String[] args) {
        if (!requireAdmin(player)) return;
        if (args.length < 3) {
            player.sendMessage(Component.translatable("hecate.arena.setup.weapon_usage"));
            return;
        }
        String traitlineId = args[2];
        Traitline traitline = Hecate.getInstance().getTraitline(traitlineId);
        if (traitline == null) {
            player.sendMessage(Component.translatable("hecate.arena.setup.weapon_unknown_traitline", Component.text(traitlineId)));
            return;
        }
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            player.sendMessage(Component.translatable("hecate.arena.setup.weapon_empty"));
            return;
        }
        try {
            Hecate.getInstance().getArenaManager().saveWeapon(traitline.getId(), item);
            player.sendMessage(Component.translatable("hecate.arena.setup.weapon_set", Component.text(traitline.getId()), Component.text(item.getType().name())));
        } catch (IOException e) {
            player.sendMessage(Component.translatable("hecate.arena.setup.save_failed", Component.text(e.getMessage())));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 2) {
            completions.addAll(List.of("join", "leave", "status", "rating", "top", "create", "edit", "setmode", "setcontrol", "setspawn", "addpoint", "payload", "setlobby", "save", "list", "enable", "weapon"));
        } else if (args.length == 3 && args[1].equalsIgnoreCase("join")) {
            completions.addAll(List.of("ranked", "unranked"));
        } else if (args.length == 4 && args[1].equalsIgnoreCase("join")) {
            completions.addAll(List.of("1v1", "2v2", "3v3", "4v4", "5v5"));
        } else if (args.length == 5 && args[1].equalsIgnoreCase("join")) {
            completions.addAll(List.of("control", "escort", "any"));
        } else if (args.length == 3 && args[1].equalsIgnoreCase("top")) {
            completions.addAll(List.of("10", "25", "50"));
        } else if (args.length == 3 && args[1].equalsIgnoreCase("weapon")) {
            Hecate.getInstance().getTraitlines().stream()
                    .sorted(Comparator.comparing(traitline -> traitline.getId().toLowerCase()))
                    .forEach(traitline -> completions.add(traitline.getId()));
        } else if (args.length == 3 && args[1].equalsIgnoreCase("edit")) {
            Hecate.getInstance().getArenaManager().getArenas().stream()
                    .sorted(Comparator.comparing(ArenaDefinition::getId))
                    .forEach(arena -> completions.add(arena.getId()));
        }
        return completions;
    }
}
