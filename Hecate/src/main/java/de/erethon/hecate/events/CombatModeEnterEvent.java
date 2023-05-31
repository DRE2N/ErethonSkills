package de.erethon.hecate.events;

import de.erethon.hecate.casting.HPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class CombatModeEnterEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final HPlayer hPlayer;
    private final CombatModeReason reason;

    public CombatModeEnterEvent(Player player, HPlayer hPlayer, CombatModeReason reason) {
        this.player = player;
        this.hPlayer = hPlayer;
        this.reason = reason;

    }

    public Player getPlayer() {
        return player;
    }

    public HPlayer getHPlayer() {
        return hPlayer;
    }

    public CombatModeReason getReason() {
        return reason;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
}
