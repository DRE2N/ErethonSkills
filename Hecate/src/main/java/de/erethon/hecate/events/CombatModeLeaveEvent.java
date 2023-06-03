package de.erethon.hecate.events;

import de.erethon.hecate.casting.HCharacter;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class CombatModeLeaveEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final HCharacter hCharacter;
    private final CombatModeReason reason;

    public CombatModeLeaveEvent(Player player, HCharacter hCharacter, CombatModeReason reason) {
        this.player = player;
        this.hCharacter = hCharacter;
        this.reason = reason;
    }

    public Player getPlayer() {
        return player;
    }

    public HCharacter getHPlayer() {
        return hCharacter;
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
