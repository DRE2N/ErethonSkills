package de.erethon.spellbook.traits.paladin;

import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlotGroup;

public class IronArmor extends SpellTrait {

    private final NamespacedKey key = new NamespacedKey("spellbook", "traitironarmor");
    private final double percent = data.getDouble("percent", 0.5);

    private AttributeModifier modifier;

    public IronArmor(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onAdd() {
        double magicRes = caster.getAttribute(Attribute.RES_MAGIC).getValue() * percent;
        modifier = new AttributeModifier(key, magicRes, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY);
        caster.getAttribute(Attribute.RES_PHYSICAL).addTransientModifier(modifier);
    }

    @Override
    protected void onRemove() {
        caster.getAttribute(Attribute.RES_PHYSICAL).removeModifier(modifier);
    }
}
