package de.erethon.spellbook.traits.warrior;

import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;

public class QuickStrokes extends SpellTrait {

    private final AttributeModifier attackSpeedModifier = new AttributeModifier("QuickStrokesSpeed", data.getDouble("attackSpeedMultiplier", 1.1), AttributeModifier.Operation.ADD_SCALAR);
    private final AttributeModifier healthModifier = new AttributeModifier("QuickStrokesHealth", data.getDouble("healthMultiplier", 0.8), AttributeModifier.Operation.ADD_SCALAR);

    public QuickStrokes(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onAdd() {
        caster.getAttribute(org.bukkit.attribute.Attribute.GENERIC_ATTACK_SPEED).addTransientModifier(attackSpeedModifier);
        caster.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).addTransientModifier(healthModifier);
    }

    @Override
    protected void onRemove() {
        caster.getAttribute(org.bukkit.attribute.Attribute.GENERIC_ATTACK_SPEED).removeModifier(attackSpeedModifier);
        caster.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).removeModifier(healthModifier);
    }
}
