package de.erethon.spellbook.effects;

import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellEffect;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;

/**
 * @author Fyreum
 */
public class SlowEffect extends SpellEffect {

    AttributeInstance instance;
    AttributeModifier modifier;
    double strength;

    public SlowEffect(EffectData data, LivingEntity caster, LivingEntity target, int duration, int stacks) {
        super(data, caster, target, duration, stacks);
        instance = target.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        strength = data.getDouble("strength", 0.2);
    }

    @Override
    public void onApply() {
        modifier = new org.bukkit.attribute.AttributeModifier("slow", -strength, org.bukkit.attribute.AttributeModifier.Operation.MULTIPLY_SCALAR_1);
        instance.addTransientModifier(modifier);
    }

    @Override
    public void onRemove() {
        instance.removeModifier(modifier);
    }
}
