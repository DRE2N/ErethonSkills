package de.erethon.hecate.casting;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.user.UserCache;
import de.erethon.hecate.Hecate;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * @author Fyreum
 */
public class HPlayerCache extends UserCache<HPlayer> {

    public HPlayerCache(@NotNull JavaPlugin plugin) {
        super(plugin);
        setUnloadAfter(0);
        loadAll();
    }

    @Override
    protected HPlayer getNewInstance(@NotNull OfflinePlayer offlinePlayer) {
        Player player = offlinePlayer.getPlayer();
        if (player != null) {
            MessageUtil.log("Loading player " + player.getUniqueId() + " (" + player.getName() + ")");
            return new HPlayer(Hecate.getInstance().getSpellbook(), player);
        }
        return null;
    }

    @Contract("_ -> new")
    public static @NotNull File getPlayerFile(@NotNull Player player) {
        return new File(Hecate.PLAYERS, player.getUniqueId() + ".yml");
    }
}
