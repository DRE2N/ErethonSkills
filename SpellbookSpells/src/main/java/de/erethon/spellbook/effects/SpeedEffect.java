package de.erethon.spellbook.effects;

import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellEffect;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlotGroup;

public class SpeedEffect extends SpellEffect {

    private final NamespacedKey key = new NamespacedKey("spellbook", "speed");

    AttributeInstance instance;
    AttributeModifier modifier;
    double value = 0.0;

    public SpeedEffect(EffectData data, LivingEntity caster, LivingEntity target, int duration, int stacks) {
        super(data, caster, target, duration, stacks);
        instance = target.getAttribute(Attribute.MOVEMENT_SPEED);
    }

    @Override
    public void onApply() {
        if (instance == null) {
            return;
        }
        removeAppliedModifier();
        value = data.getDouble("bonus", 0.2);
        modifier = new AttributeModifier(key, value, org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY);
        instance.addTransientModifier(modifier);
    }

    @Override
    public void onRemove() {
        removeAppliedModifier();
        modifier = null;
    }

    private void removeAppliedModifier() {
        if (instance == null) {
            return;
        }
        instance.getModifiers().stream()
                .filter(applied -> applied.getKey().equals(key))
                .toList()
                .forEach(instance::removeModifier);
    }
}
