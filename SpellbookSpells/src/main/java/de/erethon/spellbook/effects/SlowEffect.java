package de.erethon.spellbook.effects;

import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellEffect;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;

/**
 * @author Fyreum
 */
public class SlowEffect extends SpellEffect {

    AttributeInstance instance;
    double strength;

    public SlowEffect(EffectData data, LivingEntity caster, LivingEntity target, int duration, int stacks) {
        super(data, caster, target, duration, stacks);
        instance = target.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        strength = data.getDouble("strength", 0.2);
    }

    @Override
    public void onApply() {
        instance.setBaseValue(instance.getBaseValue() * (1 - strength));
    }

    @Override
    public void onRemove() {
        instance.setBaseValue(instance.getBaseValue() * (1 + strength));
    }
}
