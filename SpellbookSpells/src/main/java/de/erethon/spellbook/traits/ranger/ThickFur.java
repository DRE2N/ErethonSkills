package de.erethon.spellbook.traits.ranger;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import de.erethon.spellbook.events.PetSpawnEvent;
import de.erethon.spellbook.spells.ranger.pet.RangerPet;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

public class ThickFur extends SpellTrait implements Listener {

    private final AttributeModifier modifier = new AttributeModifier("thickfur", data.getDouble("bonusHealth", 100), AttributeModifier.Operation.ADD_NUMBER);
    public ThickFur(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @EventHandler
    public void onPetSpawn(PetSpawnEvent event) {
        RangerPet pet = event.getPet();
        if (!(event.getPet().getBukkitOwner() == caster)) return;
        pet.getBukkitLivingEntity().getAttribute(Attribute.GENERIC_MAX_HEALTH).addModifier(modifier);
    }

    @Override
    protected void onAdd() {
        Bukkit.getPluginManager().registerEvents(this, Spellbook.getInstance().getImplementer());
    }

    @Override
    protected void onRemove() {
        HandlerList.unregisterAll(this);
    }
}