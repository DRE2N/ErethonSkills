package de.erethon.spellbook.traits.paladin;

import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlotGroup;

public class Judgement extends SpellTrait {

    private final NamespacedKey key = new NamespacedKey("spellbook", "traitjudgement");
    private final AttributeModifier defensePercentage = new AttributeModifier(key, data.getDouble("defenseMultiplier", 0.8), AttributeModifier.Operation.ADD_SCALAR, EquipmentSlotGroup.ANY);
    private final AttributeModifier offensePercentage = new AttributeModifier(key, data.getDouble("offenseMultiplier", 1.2), AttributeModifier.Operation.ADD_SCALAR, EquipmentSlotGroup.ANY);

    public Judgement(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onAdd() {
        caster.getAttribute(Attribute.ADVANTAGE_PHYSICAL).addTransientModifier(offensePercentage);
        caster.getAttribute(Attribute.ADVANTAGE_MAGICAL).addTransientModifier(offensePercentage);
        caster.getAttribute(Attribute.RESISTANCE_MAGICAL).addTransientModifier(defensePercentage);
        caster.getAttribute(Attribute.RESISTANCE_PHYSICAL).addTransientModifier(defensePercentage);
    }

    @Override
    protected void onRemove() {
        caster.getAttribute(Attribute.ADVANTAGE_PHYSICAL).removeModifier(offensePercentage);
        caster.getAttribute(Attribute.ADVANTAGE_MAGICAL).removeModifier(offensePercentage);
        caster.getAttribute(Attribute.RESISTANCE_MAGICAL).removeModifier(defensePercentage);
        caster.getAttribute(Attribute.RESISTANCE_PHYSICAL).removeModifier(defensePercentage);
    }
}
