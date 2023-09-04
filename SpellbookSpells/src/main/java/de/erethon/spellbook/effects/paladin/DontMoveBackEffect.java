package de.erethon.spellbook.effects.paladin;

import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellEffect;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;

public class DontMoveBackEffect extends SpellEffect {

    private AttributeModifier defenseMod = new AttributeModifier("DontMoveBack", data.getDouble("defenseModifier", 0.2), AttributeModifier.Operation.MULTIPLY_SCALAR_1);

    public DontMoveBackEffect(EffectData data, LivingEntity caster, LivingEntity target, int duration, int stacks) {
        super(data, caster, target, duration, stacks);
    }

    @Override
    public void onApply() {
        target.getAttribute(Attribute.RES_MAGIC).addModifier(defenseMod);
        target.getAttribute(Attribute.RES_PHYSICAL).addModifier(defenseMod);
    }

    @Override
    public void onRemove() {
        target.getAttribute(Attribute.RES_MAGIC).removeModifier(defenseMod);
        target.getAttribute(Attribute.RES_PHYSICAL).removeModifier(defenseMod);
    }
}
