package de.erethon.spellbook.effects;

import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellEffect;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;

public class FuryEffect extends SpellEffect {

    AttributeInstance instance;
    double value = 0.0; // Important to make sure that only modifications from this effect are applied/removed!

    public FuryEffect(EffectData data, LivingEntity target, int duration, int stacks) {
        super(data, target, duration, stacks);
        instance = target.getAttribute(Attribute.GENERIC_ATTACK_SPEED);
    }

    @Override
    public void onApply() {
        value = data.getDouble("bonus", 1.0) * stacks;
        instance.setBaseValue(instance.getBaseValue() + value);
    }

    @Override
    public void onRemove() {
        instance.setBaseValue(instance.getBaseValue() - value);
    }
}

