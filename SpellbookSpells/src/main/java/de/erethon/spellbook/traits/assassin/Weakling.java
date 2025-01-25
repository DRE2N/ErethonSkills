package de.erethon.spellbook.traits.assassin;

import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlotGroup;

public class Weakling extends SpellTrait {

    private final NamespacedKey key = new NamespacedKey("spellbook", "traitweakling");
    private final AttributeModifier speedModifier = new AttributeModifier(key, data.getDouble("speedBonus"), AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY);
    private final AttributeModifier healthBonus = new AttributeModifier(key, data.getDouble("healthBonus"), AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY);

    public Weakling(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onAdd() {
        caster.getAttribute(Attribute.MOVEMENT_SPEED).addTransientModifier(speedModifier);
        caster.getAttribute(Attribute.MAX_HEALTH).addTransientModifier(healthBonus);
    }

    @Override
    protected void onRemove() {
        caster.getAttribute(Attribute.MOVEMENT_SPEED).removeModifier(speedModifier);
        caster.getAttribute(Attribute.MAX_HEALTH).removeModifier(healthBonus);
    }
}
