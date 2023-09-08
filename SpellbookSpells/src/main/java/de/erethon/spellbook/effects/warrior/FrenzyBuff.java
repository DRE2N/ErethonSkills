package de.erethon.spellbook.effects.warrior;

import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellEffect;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;

public class FrenzyBuff extends SpellEffect {

    private final double bonusDamage = data.getDouble("bonusDamage", 5);
    private AttributeModifier modifier;

    public FrenzyBuff(EffectData data, LivingEntity caster, LivingEntity target, int duration, int stacks) {
        super(data, caster, target, duration, stacks);
    }

    @Override
    public void onApply() {
        if (modifier != null) {
            target.getAttribute(Attribute.ADV_PHYSICAL).removeModifier(modifier);
        }
        modifier = new AttributeModifier("FrenzyBuff", bonusDamage * stacks, AttributeModifier.Operation.ADD_NUMBER);
        target.getAttribute(Attribute.ADV_PHYSICAL).addTransientModifier(modifier);
    }

    @Override
    public void onRemove() {
        target.getAttribute(Attribute.ADV_PHYSICAL).removeModifier(modifier);
    }
}
