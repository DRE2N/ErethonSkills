package de.erethon.spellbook.traits.paladin;

import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlotGroup;

public class IncreasedRegeneration extends SpellTrait {

    private final NamespacedKey key = new NamespacedKey("spellbook", "traitincreasedregeneration");
    private final AttributeModifier regenBonus = new AttributeModifier(key, data.getDouble("bonus", 0.5), AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY);

    public IncreasedRegeneration(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onAdd() {
        caster.getAttribute(Attribute.STAT_HEALTH_REGEN).addTransientModifier(regenBonus);
    }

    @Override
    protected void onRemove() {
        caster.getAttribute(Attribute.STAT_HEALTH_REGEN).removeModifier(regenBonus);
    }
}
