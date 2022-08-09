package de.erethon.spellbook.effects;

import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellEffect;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;

public class SpeedEffect extends SpellEffect {

    AttributeInstance instance;
    double value = 0.0;

    public SpeedEffect(EffectData data, LivingEntity caster, LivingEntity target, int duration, int stacks) {
        super(data, caster, target, duration, stacks);
        instance = target.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
    }

    @Override
    public void onApply() {
        value = data.getDouble("bonus", 0.2);
        instance.setBaseValue(instance.getBaseValue() + value);
    }

    @Override
    public void onRemove() {
        instance.setBaseValue(instance.getBaseValue() - value);
    }
}
