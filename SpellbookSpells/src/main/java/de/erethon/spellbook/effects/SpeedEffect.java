package de.erethon.spellbook.effects;

import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellEffect;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;

public class SpeedEffect extends SpellEffect {

    AttributeInstance instance;
    AttributeModifier modifier;
    double value = 0.0;

    public SpeedEffect(EffectData data, LivingEntity caster, LivingEntity target, int duration, int stacks) {
        super(data, caster, target, duration, stacks);
        instance = target.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
    }

    @Override
    public void onApply() {
        value = data.getDouble("bonus", 0.2);
        modifier = new org.bukkit.attribute.AttributeModifier("speed", value, org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER);
        instance.addModifier(modifier);
    }

    @Override
    public void onRemove() {
        instance.removeModifier(modifier);
    }
}
