package de.erethon.spellbook.events;

import de.erethon.spellbook.spells.ranger.pet.RangerPet;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class PetAttackEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private final RangerPet pet;
    private final LivingEntity target;

    public PetAttackEvent(RangerPet pet, LivingEntity target) {
        this.pet = pet;
        this.target = target;
    }

    public RangerPet getPet() {
        return pet;
    }

    public LivingEntity getTarget() {
        return target;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
}

