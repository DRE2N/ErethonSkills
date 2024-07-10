package de.erethon.spellbook.traits.warrior;

import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlotGroup;

public class Assault extends SpellTrait {

    private final NamespacedKey key = new NamespacedKey("spellbook", "traitassault");
    private final AttributeModifier speedModifier = new AttributeModifier(key, data.getDouble("bonusSpeed", 0.1), AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY);
    private final AttributeModifier physicalModifier = new AttributeModifier(key, data.getDouble("bonusPhysical", 25), AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY);

    public Assault(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onAdd() {
        caster.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).addTransientModifier(speedModifier);
        caster.getAttribute(Attribute.ADV_PHYSICAL).addTransientModifier(physicalModifier);
    }

    @Override
    protected void onRemove() {
        caster.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).removeModifier(speedModifier);
        caster.getAttribute(Attribute.ADV_PHYSICAL).removeModifier(physicalModifier);
    }
}
