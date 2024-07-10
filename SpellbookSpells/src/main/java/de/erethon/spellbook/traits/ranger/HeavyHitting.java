package de.erethon.spellbook.traits.ranger;

import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlotGroup;

public class HeavyHitting extends SpellTrait {

    private final NamespacedKey key = new NamespacedKey("spellbook", "traitheavyhitting");
    private final AttributeModifier modifier = new AttributeModifier(key, data.getDouble("bonus", 20), AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY);

    public HeavyHitting(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onAdd() {
        caster.getAttribute(Attribute.PEN_PHYSICAL).addTransientModifier(modifier);
        caster.getAttribute(Attribute.PEN_MAGIC).addTransientModifier(modifier);
    }

    @Override
    protected void onRemove() {
        caster.getAttribute(Attribute.PEN_PHYSICAL).removeModifier(modifier);
        caster.getAttribute(Attribute.PEN_MAGIC).removeModifier(modifier);
    }
}
