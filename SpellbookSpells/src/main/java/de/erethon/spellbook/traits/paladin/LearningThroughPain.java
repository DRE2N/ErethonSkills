package de.erethon.spellbook.traits.paladin;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlotGroup;

public class LearningThroughPain extends SpellTrait {

    private final NamespacedKey key = new NamespacedKey("spellbook", "traitlearningthroughpain");
    private final AttributeModifier advAttributeModifier = new AttributeModifier(key, data.getDouble("advAttributeMultiplier", 0.95), AttributeModifier.Operation.ADD_SCALAR, EquipmentSlotGroup.ANY);
    private final double incomingDamageMultiplier = data.getDouble("incomingDamageMultiplier", 0.85);

    public LearningThroughPain(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onAdd() {
        caster.getAttribute(Attribute.ADVANTAGE_PHYSICAL).addTransientModifier(advAttributeModifier);
        caster.getAttribute(Attribute.ADVANTAGE_MAGICAL).addTransientModifier(advAttributeModifier);
        caster.getAttribute(Attribute.ATTACK_DAMAGE).addTransientModifier(advAttributeModifier);
    }

    @Override
    protected void onRemove() {
        caster.getAttribute(Attribute.ADVANTAGE_PHYSICAL).removeModifier(advAttributeModifier);
        caster.getAttribute(Attribute.ADVANTAGE_MAGICAL).removeModifier(advAttributeModifier);
        caster.getAttribute(Attribute.ATTACK_DAMAGE).removeModifier(advAttributeModifier);
    }

    @Override
    public double onDamage(LivingEntity attacker, double damage, PDamageType type) {
        return damage * incomingDamageMultiplier;
    }
}
