package de.erethon.hecate.casting;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.user.UserCache;
import de.erethon.hecate.Hecate;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Fyreum
 */
public class HPlayerCache implements Listener {

    private final Set<HPlayer> players = new HashSet<>();

    public HPlayerCache(@NotNull JavaPlugin plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public HPlayer getNewInstance(@NotNull OfflinePlayer offlinePlayer) {
        Player player = offlinePlayer.getPlayer();
        if (player != null) {
            MessageUtil.log("Loading player " + player.getUniqueId() + " (" + player.getName() + ")");
            HPlayer hPlayer = new HPlayer(player);
            players.add(hPlayer);
            return hPlayer;
        }
        return null;
    }

    public void remove(@NotNull HPlayer hPlayer) {
        players.remove(hPlayer);
    }

    public HPlayer getByPlayer(@NotNull Player player) {
        for (HPlayer hPlayer : players) {
            if (hPlayer.getPlayer().getUniqueId().equals(player.getUniqueId())) {
                return hPlayer;
            }
        }
        return getNewInstance(player);
    }

    public HPlayer getByName(@NotNull String name) {
        for (HPlayer hPlayer : players) {
            if (hPlayer.getPlayer().getName().equalsIgnoreCase(name)) {
                return hPlayer;
            }
        }
        return null;
    }

    @Contract("_ -> new")
    public static @NotNull File getPlayerFile(@NotNull Player player) {
        return new File(Hecate.PLAYERS, player.getUniqueId() + ".yml");
    }

    public @Nullable HCharacter getCharacter(@NotNull Player player) {
        return getByPlayer(player).getSelectedCharacter();
    }

    public @Nullable HCharacter getCharacter(@NotNull String player) {
        return getByName(player).getSelectedCharacter();
    }

    public Set<HPlayer> getPlayers() {
        return players;
    }
}
