package de.erethon.spellbook.traits.assassin;

import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlotGroup;

public class HardTraining extends SpellTrait {

    private final NamespacedKey key = new NamespacedKey("spellbook", "traithardtraining");
    private final AttributeModifier modifier = new AttributeModifier(key, data.getDouble("healthBonus", 100), AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY);

    public HardTraining(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onAdd() {
        caster.getAttribute(Attribute.GENERIC_MAX_HEALTH).addTransientModifier(modifier);
    }

    @Override
    protected void onRemove() {
        caster.getAttribute(Attribute.GENERIC_MAX_HEALTH).removeModifier(modifier);
    }
}
