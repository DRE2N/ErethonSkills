package de.erethon.spellbook.traits.warrior;

import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlotGroup;

public class QuickStrokes extends SpellTrait {

    private final NamespacedKey key = new NamespacedKey("spellbook", "traitquickstrokes");
    private final AttributeModifier attackSpeedModifier = new AttributeModifier(key, data.getDouble("attackSpeedMultiplier", 1.1), AttributeModifier.Operation.ADD_SCALAR, EquipmentSlotGroup.ANY);
    private final AttributeModifier healthModifier = new AttributeModifier(key, data.getDouble("healthMultiplier", 0.8), AttributeModifier.Operation.ADD_SCALAR, EquipmentSlotGroup.ANY);

    public QuickStrokes(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onAdd() {
        caster.getAttribute(org.bukkit.attribute.Attribute.GENERIC_ATTACK_SPEED).addTransientModifier(attackSpeedModifier);
        caster.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).addTransientModifier(healthModifier);
    }

    @Override
    protected void onRemove() {
        caster.getAttribute(org.bukkit.attribute.Attribute.GENERIC_ATTACK_SPEED).removeModifier(attackSpeedModifier);
        caster.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).removeModifier(healthModifier);
    }
}
