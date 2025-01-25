package de.erethon.spellbook.effects.warrior;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellEffect;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;

public class BuffLastStand extends SpellEffect {

    private double value = 0.0;

    public BuffLastStand(EffectData data, LivingEntity caster, LivingEntity target, int duration, int stacks) {
        super(data, caster, target, duration, stacks);
    }

    @Override
    public void onApply() {
        value = Spellbook.getScaledValue(data, caster, target, Attribute.RESISTANCE_PHYSICAL);
        target.getAttribute(Attribute.RESISTANCE_PHYSICAL).setBaseValue(target.getAttribute(Attribute.RESISTANCE_PHYSICAL).getBaseValue() + value);
        target.getAttribute(Attribute.RESISTANCE_MAGICAL).setBaseValue(target.getAttribute(Attribute.RESISTANCE_MAGICAL).getBaseValue() + value);
    }

    @Override
    public void onRemove() {
        target.getAttribute(Attribute.RESISTANCE_PHYSICAL).setBaseValue(target.getAttribute(Attribute.RESISTANCE_PHYSICAL).getBaseValue() - value);
        target.getAttribute(Attribute.RESISTANCE_MAGICAL).setBaseValue(target.getAttribute(Attribute.RESISTANCE_MAGICAL).getBaseValue() - value);
    }
}
