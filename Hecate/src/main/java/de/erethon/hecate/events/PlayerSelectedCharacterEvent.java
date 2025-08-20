package de.erethon.hecate.events;

import de.erethon.hecate.data.HCharacter;
import de.erethon.hecate.data.HPlayer;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public class PlayerSelectedCharacterEvent extends PlayerEvent {

    private static final HandlerList HANDLERS = new HandlerList();
    private final HPlayer player;
    private final HCharacter character;
    private final boolean isNewCharacter;

    public PlayerSelectedCharacterEvent(HPlayer hPlayer, HCharacter character, boolean isNewCharacter) {
        super(hPlayer.getPlayer());
        this.player = hPlayer;
        this.character = character;
        this.isNewCharacter = isNewCharacter;
    }

    public HPlayer getHPlayer() {
        return player;
    }

    public HCharacter getCharacter() {
        return character;
    }

    // If this character was newly created by the player
    // and not selected from existing characters
    public boolean isNewCharacter() {
        return isNewCharacter;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
