package de.erethon.spellbook.effects;

import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellEffect;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlotGroup;

/**
 * @author Fyreum
 */
public class SlowEffect extends SpellEffect {

    private final NamespacedKey key = new NamespacedKey("spellbook", "slow");

    AttributeInstance instance;
    AttributeModifier modifier;
    double strength;

    public SlowEffect(EffectData data, LivingEntity caster, LivingEntity target, int duration, int stacks) {
        super(data, caster, target, duration, stacks);
        instance = target.getAttribute(Attribute.MOVEMENT_SPEED);
        strength = data.getDouble("strength", 0.2);
    }

    @Override
    public void onApply() {
        modifier = new AttributeModifier(key, -strength, org.bukkit.attribute.AttributeModifier.Operation.MULTIPLY_SCALAR_1, EquipmentSlotGroup.ANY);
        if (!instance.getModifiers().contains(modifier)) {
            instance.addTransientModifier(modifier);
        }
    }

    @Override
    public void onRemove() {
        instance.removeModifier(modifier);
    }
}
