package de.erethon.spellbook.utils;

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.spells.ranger.beastmaster.pet.RangerPet;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.HashMap;

public class PetLookup implements Listener {

    private final HashMap<LivingEntity, RangerPet> pets = new HashMap<>();

    public PetLookup() {
        Bukkit.getPluginManager().registerEvents(this, Spellbook.getInstance().getImplementer());
    }

    public void add(LivingEntity living, RangerPet pet) {
        pets.put(living, pet);
    }

    public RangerPet get(LivingEntity living) {
        return pets.get(living);
    }

    public void remove(LivingEntity living) {
        pets.remove(living);
    }

    @EventHandler
    public void onEntityRemove(EntityRemoveFromWorldEvent event) {
        if (event.getEntity() instanceof LivingEntity living) {
            pets.remove(living);
        }
    }
}
