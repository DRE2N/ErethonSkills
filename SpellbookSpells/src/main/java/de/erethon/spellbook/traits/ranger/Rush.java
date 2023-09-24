package de.erethon.spellbook.traits.ranger;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import de.erethon.spellbook.events.PetSpawnEvent;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

public class Rush extends SpellTrait implements Listener {

    private final AttributeModifier healthModifier = new AttributeModifier("RushHealth", data.getDouble("healthBonus", 50), AttributeModifier.Operation.ADD_NUMBER);
    private final AttributeModifier speedModifier = new AttributeModifier("RushSpeed", data.getDouble("speedBonus", 0.3), AttributeModifier.Operation.ADD_NUMBER);

    public Rush(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @EventHandler
    public void onPetSpawn(PetSpawnEvent event) {
        event.getPet().getBukkitLivingEntity().getAttribute(Attribute.GENERIC_MAX_HEALTH).addTransientModifier(healthModifier);
        event.getPet().getBukkitLivingEntity().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).addTransientModifier(speedModifier);
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