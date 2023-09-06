package de.erethon.spellbook.traits.paladin;

import de.erethon.papyrus.DamageType;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;

public class LearningThroughPain extends SpellTrait {

    private final AttributeModifier advAttributeModifier = new AttributeModifier("LearningThroughPain", data.getDouble("advAttributeMultiplier", 0.95), AttributeModifier.Operation.ADD_SCALAR);
    private final double incomingDamageMultiplier = data.getDouble("incomingDamageMultiplier", 0.85);

    public LearningThroughPain(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onAdd() {
        caster.getAttribute(Attribute.ADV_PHYSICAL).addTransientModifier(advAttributeModifier);
        caster.getAttribute(Attribute.ADV_MAGIC).addTransientModifier(advAttributeModifier);
        caster.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).addTransientModifier(advAttributeModifier);
    }

    @Override
    protected void onRemove() {
        caster.getAttribute(Attribute.ADV_PHYSICAL).removeModifier(advAttributeModifier);
        caster.getAttribute(Attribute.ADV_MAGIC).removeModifier(advAttributeModifier);
        caster.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).removeModifier(advAttributeModifier);
    }

    @Override
    public double onDamage(LivingEntity attacker, double damage, DamageType type) {
        return damage * incomingDamageMultiplier;
    }
}
