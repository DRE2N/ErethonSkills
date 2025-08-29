package de.erethon.hecate.charselection;

import de.erethon.hecate.Hecate;
import de.erethon.hecate.data.DatabaseManager;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class BaseSelection implements Listener {

    protected final Hecate plugin = Hecate.getInstance();
    protected final DatabaseManager databaseManager = plugin.getDatabaseManager();
    protected final Player player;
    protected List<BaseDisplay> displayed = new ArrayList<>();
    protected final Interaction[] interactions = new Interaction[9];
    protected final Set<TextDisplay> emptySlotDisplays = new HashSet<>();
    protected boolean playerIsDone = false;
    protected boolean confirmed = false;

    public BaseSelection(Player player) {
        this.player = player;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public abstract void onRightClick(BaseDisplay display);
    public abstract void onLeftClick(BaseDisplay display);

    protected abstract void setup();

    public void done() {
        HandlerList.unregisterAll(this);
    }

    @EventHandler
    private void onPreCommand(PlayerCommandPreprocessEvent event) {
        if (playerIsDone) {
            return;
        }
        if (event.getPlayer() != player) {
            return;
        }
        String message = event.getMessage();
        if (message.startsWith("/")) {
            event.setCancelled(true);
            player.sendMessage(Component.translatable("hecate.combat.no_commands_in_selection"));
        }
    }


}
