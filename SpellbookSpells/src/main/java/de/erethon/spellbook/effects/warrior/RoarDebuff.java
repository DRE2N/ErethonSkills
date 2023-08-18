package de.erethon.spellbook.effects.warrior;

import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellEffect;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;

public class RoarDebuff extends SpellEffect {

    private final AttributeModifier modifier = new AttributeModifier("RoarDebuff", data.getDouble("debuffAmount", 0.75f), AttributeModifier.Operation.MULTIPLY_SCALAR_1);

    public RoarDebuff(EffectData data, LivingEntity caster, LivingEntity target, int duration, int stacks) {
        super(data, caster, target, duration, stacks);
    }

    @Override
    public void onApply() {
        target.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).addTransientModifier(modifier);
        target.getAttribute(Attribute.ADV_PHYSICAL).addTransientModifier(modifier);
    }

    @Override
    public void onRemove() {
        target.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).removeModifier(modifier);
        target.getAttribute(Attribute.ADV_PHYSICAL).removeModifier(modifier);
    }
}
