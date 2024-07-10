package de.erethon.spellbook.traits.warrior;

import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlotGroup;

public class Challenger extends SpellTrait {

    private final NamespacedKey key = new NamespacedKey("spellbook", "traitchallenger");
    private final AttributeModifier damageModifier = new AttributeModifier(key, data.getDouble("damageMultiplier", 1.1), AttributeModifier.Operation.ADD_SCALAR, EquipmentSlotGroup.ANY);
    private final AttributeModifier healthModifier = new AttributeModifier(key, data.getDouble("healthMultiplier", 1.2), AttributeModifier.Operation.ADD_SCALAR, EquipmentSlotGroup.ANY);

    public Challenger(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onAdd() {
        caster.getAttribute(Attribute.ADV_PHYSICAL).addTransientModifier(damageModifier);
        caster.getAttribute(Attribute.GENERIC_MAX_HEALTH).addTransientModifier(healthModifier);
    }

    @Override
    protected void onRemove() {
        caster.getAttribute(Attribute.ADV_PHYSICAL).removeModifier(damageModifier);
        caster.getAttribute(Attribute.GENERIC_MAX_HEALTH).removeModifier(healthModifier);
    }
}
