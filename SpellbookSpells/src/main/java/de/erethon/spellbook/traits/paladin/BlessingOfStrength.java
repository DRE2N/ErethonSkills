package de.erethon.spellbook.traits.paladin;

import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlotGroup;

public class BlessingOfStrength extends SpellTrait {

    private final NamespacedKey key = new NamespacedKey("spellbook", "traitblessingofstrength");
    private final AttributeModifier modifier = new AttributeModifier(key, data.getDouble("bonus", 50), AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY);

    public BlessingOfStrength(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onAdd() {
        caster.getAttribute(Attribute.ADV_PHYSICAL).addTransientModifier(modifier);
        caster.getAttribute(Attribute.ADV_MAGIC).addTransientModifier(modifier);
    }

    @Override
    protected void onRemove() {
        caster.getAttribute(Attribute.ADV_PHYSICAL).removeModifier(modifier);
        caster.getAttribute(Attribute.ADV_MAGIC).removeModifier(modifier);
    }
}
