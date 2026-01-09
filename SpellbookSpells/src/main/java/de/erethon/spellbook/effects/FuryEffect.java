package de.erethon.spellbook.effects;

import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellEffect;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlotGroup;

public class FuryEffect extends SpellEffect {

    private final NamespacedKey key = new NamespacedKey("spellbook", "fury");

    AttributeInstance instance;
    AttributeModifier modifier;
    double value = 0.0; // Important to make sure that only modifications from this effect are applied/removed!

    public FuryEffect(EffectData data, LivingEntity caster, LivingEntity target, int duration, int stacks) {
        super(data, caster, target, duration, stacks);
        instance = target.getAttribute(Attribute.ATTACK_SPEED);
    }

    @Override
    public void onApply() {
        value = data.getDouble("bonus", 1.0) * stacks;
        modifier = new AttributeModifier(key, value, org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY);
        if (!instance.getModifiers().contains(modifier)) {
            instance.addTransientModifier(modifier);
        }
    }

    @Override
    public void onRemove() {
        instance.removeModifier(modifier);
    }
}

