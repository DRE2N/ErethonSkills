package de.erethon.spellbook.traits.warrior;

import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;

public class Challenger extends SpellTrait {

    private final AttributeModifier damageModifier = new AttributeModifier("OutperformDmg", data.getDouble("damageMultiplier", 1.1), AttributeModifier.Operation.ADD_SCALAR);
    private final AttributeModifier healthModifier = new AttributeModifier("OutperformHealth", data.getDouble("healthMultiplier", 1.2), AttributeModifier.Operation.ADD_SCALAR);

    public Challenger(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onAdd() {
        caster.getAttribute(Attribute.ADV_PHYSICAL).addTransientModifier(damageModifier);
        caster.getAttribute(Attribute.GENERIC_MAX_HEALTH).addTransientModifier(healthModifier);
    }

    @Override
    protected void onRemove() {
        caster.getAttribute(Attribute.ADV_PHYSICAL).removeModifier(damageModifier);
        caster.getAttribute(Attribute.GENERIC_MAX_HEALTH).removeModifier(healthModifier);
    }
}
