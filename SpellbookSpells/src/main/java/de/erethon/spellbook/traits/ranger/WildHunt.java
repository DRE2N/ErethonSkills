package de.erethon.spellbook.traits.ranger;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import de.erethon.spellbook.events.PetSpawnEvent;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.EquipmentSlotGroup;

public class WildHunt extends SpellTrait implements Listener {

    private final NamespacedKey key = new NamespacedKey("spellbook", "traitwildhunt");
    private final AttributeModifier healthModifier = new AttributeModifier(key, data.getDouble("healthBonus", 50), AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY);
    private final AttributeModifier dmgModifier = new AttributeModifier(key, data.getDouble("damageBonus", 5), AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY);

    public WildHunt(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @EventHandler
    public void onPetSpawn(PetSpawnEvent event) {
        event.getPet().getBukkitLivingEntity().getAttribute(Attribute.MAX_HEALTH).addTransientModifier(healthModifier);
        event.getPet().getBukkitLivingEntity().getAttribute(Attribute.MOVEMENT_SPEED).addTransientModifier(dmgModifier);
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
