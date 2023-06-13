package de.erethon.spellbook.effects;

import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellEffect;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;

public class FuryEffect extends SpellEffect {

    AttributeInstance instance;
    AttributeModifier modifier;
    double value = 0.0; // Important to make sure that only modifications from this effect are applied/removed!

    public FuryEffect(EffectData data, LivingEntity caster, LivingEntity target, int duration, int stacks) {
        super(data, caster, target, duration, stacks);
        instance = target.getAttribute(Attribute.GENERIC_ATTACK_SPEED);
    }

    @Override
    public void onApply() {
        value = data.getDouble("bonus", 1.0) * stacks;
        modifier = new org.bukkit.attribute.AttributeModifier("fury", value, org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER);
        instance.addTransientModifier(modifier);
    }

    @Override
    public void onRemove() {
        instance.removeModifier(modifier);
    }
}

