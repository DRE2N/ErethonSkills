package de.erethon.spellbook.traits.assassin;

import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlotGroup;

public class Fighter extends SpellTrait {

    private final NamespacedKey key = new NamespacedKey("spellbook", "traitfighter");
    private final AttributeModifier modifier = new AttributeModifier(key, data.getDouble("damageMultiplier", 1.2), AttributeModifier.Operation.ADD_SCALAR, EquipmentSlotGroup.ANY);

    public Fighter(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    public void onAdd() {
        caster.getAttribute(Attribute.ADV_PHYSICAL).addTransientModifier(modifier);
    }

    @Override
    public void onRemove() {
        caster.getAttribute(Attribute.ADV_PHYSICAL).removeModifier(modifier);
    }
}
