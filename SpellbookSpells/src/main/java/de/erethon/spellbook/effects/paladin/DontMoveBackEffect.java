package de.erethon.spellbook.effects.paladin;

import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellEffect;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlotGroup;

public class DontMoveBackEffect extends SpellEffect {

    private final NamespacedKey key = new NamespacedKey("spellbook", "dontmoveback");

    private AttributeModifier defenseMod = new AttributeModifier(key, data.getDouble("defenseModifier", 0.2), AttributeModifier.Operation.MULTIPLY_SCALAR_1, EquipmentSlotGroup.ANY);

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
