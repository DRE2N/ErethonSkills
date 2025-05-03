package de.erethon.spellbook.events;

import de.erethon.spellbook.spells.ranger.beastmaster.pet.RangerPet;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class PetDeathEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private final RangerPet pet;

    public PetDeathEvent(RangerPet pet) {
        this.pet = pet;
    }

    public RangerPet getPet() {
        return pet;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
}
