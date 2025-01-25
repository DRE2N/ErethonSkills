package de.erethon.spellbook.effects.warrior;

import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellEffect;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlotGroup;

public class RoarDebuff extends SpellEffect {

    private final NamespacedKey key = new NamespacedKey("spellbook", "roar");
    private final AttributeModifier modifier = new AttributeModifier(key, data.getDouble("debuffAmount", -0.25), AttributeModifier.Operation.MULTIPLY_SCALAR_1, EquipmentSlotGroup.ANY);

    public RoarDebuff(EffectData data, LivingEntity caster, LivingEntity target, int duration, int stacks) {
        super(data, caster, target, duration, stacks);
    }

    @Override
    public void onApply() {
        target.getAttribute(Attribute.ATTACK_DAMAGE).addTransientModifier(modifier);
        target.getAttribute(Attribute.ADVANTAGE_PHYSICAL).addTransientModifier(modifier);
    }

    @Override
    public void onRemove() {
        target.getAttribute(Attribute.ATTACK_DAMAGE).removeModifier(modifier);
        target.getAttribute(Attribute.ADVANTAGE_PHYSICAL).removeModifier(modifier);
    }
}
