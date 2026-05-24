package de.erethon.hecate.arenas;

import de.erethon.aergia.Aergia;
import de.erethon.aergia.group.Group;
import de.erethon.aergia.group.GroupMember;
import de.erethon.hecate.Hecate;
import de.erethon.hecate.data.HCharacter;
import de.erethon.hecate.data.HPlayer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class ArenaQueueManager {

    private final ArenaManager manager;
    private final List<ArenaQueueEntry> entries = new ArrayList<>();

    public ArenaQueueManager(ArenaManager manager) {
        this.manager = manager;
    }

    public synchronized boolean isQueued(UUID uuid) {
        return entries.stream().anyMatch(entry -> entry.playerIds().contains(uuid));
    }

    public synchronized void leave(Player player) {
        entries.removeIf(entry -> entry.playerIds().contains(player.getUniqueId()));
        player.sendMessage(Component.translatable("hecate.arena.queue.left"));
    }

    public synchronized void join(Player sender, ArenaQueueType queueType, int teamSize, ArenaMode mode) {
        if (teamSize < 1 || teamSize > 5) {
            sender.sendMessage(Component.translatable("hecate.arena.queue.invalid_bracket"));
            return;
        }
        if (manager.isInMatch(sender.getUniqueId()) || isQueued(sender.getUniqueId())) {
            sender.sendMessage(Component.translatable("hecate.arena.queue.already_queued"));
            return;
        }
        List<Player> players = resolveQueuedPlayers(sender);
        if (players == null) {
            return;
        }
        if (players.size() > teamSize) {
            sender.sendMessage(Component.translatable("hecate.arena.queue.group_size_mismatch", Component.text(players.size()), Component.text(teamSize)));
            return;
        }
        for (Player player : players) {
            if (!validatePlayer(player, sender)) {
                return;
            }
            if (manager.isInMatch(player.getUniqueId()) || isQueued(player.getUniqueId())) {
                sender.sendMessage(Component.translatable("hecate.arena.queue.member_busy", Component.text(player.getName())));
                return;
            }
        }
        ArenaQueueEntry entry = ArenaQueueEntry.of(players, queueType, teamSize, mode);
        entries.add(entry);
        for (Player player : players) {
            player.sendMessage(Component.translatable("hecate.arena.queue.joined", Component.text(queueType.name().toLowerCase()), Component.text(teamSize + "v" + teamSize)));
        }
        tryMatch(entry.queueType(), entry.teamSize());
    }

    private List<Player> resolveQueuedPlayers(Player sender) {
        if (Bukkit.getPluginManager().isPluginEnabled("Aergia")) {
            Group group = Aergia.inst().getGroupManager().getGroup(sender);
            if (group != null) {
                if (!group.getLeader().getUniqueId().equals(sender.getUniqueId())) {
                    sender.sendMessage(Component.translatable("hecate.arena.queue.only_group_leader"));
                    return null;
                }
                List<Player> players = new ArrayList<>();
                for (GroupMember member : group.getMembers()) {
                    Player player = member.getPlayer();
                    if (player == null || !player.isOnline()) {
                        sender.sendMessage(Component.translatable("hecate.arena.queue.member_offline", Component.text(member.getDisplayName())));
                        return null;
                    }
                    players.add(player);
                }
                return players;
            }
        }
        return List.of(sender);
    }

    private boolean validatePlayer(Player player, Player sender) {
        HPlayer hPlayer = Hecate.getInstance().getDatabaseManager().getHPlayer(player);
        if (hPlayer == null) {
            sender.sendMessage(Component.translatable("hecate.arena.queue.member_not_loaded", Component.text(player.getName())));
            return false;
        }
        HCharacter character = hPlayer.getSelectedCharacter();
        if (character == null || character.getHClass() == null || character.getTraitline() == null) {
            sender.sendMessage(Component.translatable("hecate.arena.queue.member_no_character", Component.text(player.getName())));
            return false;
        }
        return true;
    }

    private void tryMatch(ArenaQueueType queueType, int teamSize) {
        for (ArenaMode mode : ArenaMode.values()) {
            ArenaDefinition arena = manager.findAvailableArena(mode, teamSize);
            if (arena == null) {
                continue;
            }
            List<ArenaQueueEntry> available = entries.stream()
                    .filter(entry -> entry.queueType() == queueType)
                    .filter(entry -> entry.teamSize() == teamSize)
                    .filter(entry -> entry.mode() == null || entry.mode() == mode)
                    .sorted(Comparator.comparingLong(ArenaQueueEntry::queuedAt))
                    .toList();
            List<ArenaQueueEntry> firstTeam = findTeam(available, teamSize, new ArrayList<>());
            if (firstTeam == null) {
                continue;
            }
            List<ArenaQueueEntry> remaining = available.stream()
                    .filter(entry -> !firstTeam.contains(entry))
                    .toList();
            List<ArenaQueueEntry> secondTeam = findTeam(remaining, teamSize, new ArrayList<>());
            if (secondTeam == null) {
                continue;
            }
            entries.removeAll(firstTeam);
            entries.removeAll(secondTeam);
            manager.startMatch(
                    arena,
                    queueType,
                    ArenaQueueEntry.combined(firstTeam, queueType, teamSize, mode),
                    ArenaQueueEntry.combined(secondTeam, queueType, teamSize, mode)
            );
            return;
        }
    }

    private List<ArenaQueueEntry> findTeam(List<ArenaQueueEntry> candidates, int targetSize, List<ArenaQueueEntry> selected) {
        int selectedSize = selected.stream().mapToInt(entry -> entry.playerIds().size()).sum();
        if (selectedSize == targetSize) {
            return selected;
        }
        if (selectedSize > targetSize) {
            return null;
        }
        for (ArenaQueueEntry candidate : candidates) {
            if (selected.contains(candidate)) {
                continue;
            }
            List<ArenaQueueEntry> nextSelected = new ArrayList<>(selected);
            nextSelected.add(candidate);
            List<ArenaQueueEntry> remaining = candidates.subList(candidates.indexOf(candidate) + 1, candidates.size());
            List<ArenaQueueEntry> result = findTeam(remaining, targetSize, nextSelected);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    public synchronized String getStatus(UUID uuid) {
        for (ArenaQueueEntry entry : entries) {
            if (entry.playerIds().contains(uuid)) {
                return entry.queueType().name().toLowerCase() + " " + entry.teamSize() + "v" + entry.teamSize();
            }
        }
        return null;
    }

    public synchronized void removeInvalid(Player player) {
        entries.removeIf(entry -> entry.playerIds().contains(player.getUniqueId()));
    }
}
