package de.erethon.spellbook.effects.warrior;

import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellEffect;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlotGroup;

public class FrenzyBuff extends SpellEffect {

    private final NamespacedKey key = new NamespacedKey("spellbook", "frenzy");
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
        modifier = new AttributeModifier(key, bonusDamage * stacks, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY);
        target.getAttribute(Attribute.ADV_PHYSICAL).addTransientModifier(modifier);
    }

    @Override
    public void onRemove() {
        target.getAttribute(Attribute.ADV_PHYSICAL).removeModifier(modifier);
    }
}
