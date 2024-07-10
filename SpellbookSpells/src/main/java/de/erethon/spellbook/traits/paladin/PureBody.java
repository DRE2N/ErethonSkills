package de.erethon.spellbook.traits.paladin;

import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlotGroup;

public class PureBody extends SpellTrait {

    private final NamespacedKey key = new NamespacedKey("spellbook", "traitpurebody");
    private final AttributeModifier healthBuff = new AttributeModifier(key, data.getDouble("healthBuff", 50), AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY);
    private final AttributeModifier regenBuff = new AttributeModifier(key, data.getDouble("regenBuff", 2), AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY);

    public PureBody(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onAdd() {
        caster.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).addTransientModifier(healthBuff);
        caster.getAttribute(Attribute.STAT_HEALTHREGEN).addTransientModifier(regenBuff);
    }

    @Override
    protected void onRemove() {
        caster.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).removeModifier(healthBuff);
        caster.getAttribute(Attribute.STAT_HEALTHREGEN).removeModifier(regenBuff);
    }
}
