package de.erethon.spellbook.traits.warrior;

import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlotGroup;

public class Outperform extends SpellTrait {

    private final NamespacedKey key = new NamespacedKey("spellbook", "traitoutperform");
    private final AttributeModifier damageModifier = new AttributeModifier(key, data.getDouble("damageMultiplier", 1.1), AttributeModifier.Operation.ADD_SCALAR, EquipmentSlotGroup.ANY);
    private final AttributeModifier healthModifier = new AttributeModifier(key, data.getDouble("healthMultiplier", 1.2), AttributeModifier.Operation.ADD_SCALAR, EquipmentSlotGroup.ANY);

    public Outperform(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onAdd() {
        caster.getAttribute(Attribute.ADVANTAGE_PHYSICAL).addTransientModifier(damageModifier);
        caster.getAttribute(Attribute.MAX_HEALTH).addTransientModifier(healthModifier);
    }

    @Override
    protected void onRemove() {
        caster.getAttribute(Attribute.ADVANTAGE_PHYSICAL).removeModifier(damageModifier);
        caster.getAttribute(Attribute.MAX_HEALTH).removeModifier(healthModifier);
    }
}
