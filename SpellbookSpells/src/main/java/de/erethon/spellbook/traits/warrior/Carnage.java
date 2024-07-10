package de.erethon.spellbook.traits.warrior;

import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlotGroup;

public class Carnage extends SpellTrait {

    private final NamespacedKey key = new NamespacedKey("spellbook", "traitcarnage");
    private final AttributeModifier defense = new AttributeModifier(key, data.getDouble("resistanceMultiplier", 0.8), AttributeModifier.Operation.ADD_SCALAR, EquipmentSlotGroup.ANY);
    private final AttributeModifier offense = new AttributeModifier(key, data.getDouble("advMultiplier", 1.33), AttributeModifier.Operation.ADD_SCALAR, EquipmentSlotGroup.ANY);

    public Carnage(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onAdd() {
        caster.getAttribute(Attribute.ADV_PHYSICAL).addTransientModifier(offense);
        caster.getAttribute(Attribute.RES_PHYSICAL).addTransientModifier(defense);
    }

    @Override
    protected void onRemove() {
        caster.getAttribute(Attribute.ADV_PHYSICAL).removeModifier(offense);
        caster.getAttribute(Attribute.RES_PHYSICAL).removeModifier(defense);
    }
}
