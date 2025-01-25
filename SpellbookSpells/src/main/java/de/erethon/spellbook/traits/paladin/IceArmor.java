package de.erethon.spellbook.traits.paladin;

import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlotGroup;

public class IceArmor extends SpellTrait {

    private final double percent = data.getDouble("percent", 0.5);

    private final NamespacedKey key = new NamespacedKey("spellbook", "traiticearmor");
    private AttributeModifier modifier;

    public IceArmor(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onAdd() {
        double physRes = caster.getAttribute(Attribute.RESISTANCE_PHYSICAL).getValue() * percent;
        modifier = new AttributeModifier(key, physRes, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY);
        caster.getAttribute(Attribute.RESISTANCE_MAGICAL).addTransientModifier(modifier);
    }

    @Override
    protected void onRemove() {
        caster.getAttribute(Attribute.RESISTANCE_MAGICAL).removeModifier(modifier);
    }

}
