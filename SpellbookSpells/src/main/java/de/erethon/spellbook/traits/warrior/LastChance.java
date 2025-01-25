package de.erethon.spellbook.traits.warrior;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlotGroup;

public class LastChance extends SpellTrait {

    private final NamespacedKey key = new NamespacedKey("spellbook", "traitlastchance");
    private final double healthTriggerPercentage = data.getDouble("healthTriggerPercentage", 0.5);
    private final long duration = data.getLong("duration", 100) * 50L;
    private final AttributeModifier modifier = new AttributeModifier(key, data.getDouble("bonus", 50), AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY);
    private long addedAt = 0;

    public LastChance(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    public double onDamage(LivingEntity attacker, double damage, PDamageType type) {
        if (caster.getHealth() / caster.getMaxHealth() <= healthTriggerPercentage) {
            caster.getAttribute(Attribute.RESISTANCE_PHYSICAL).addTransientModifier(modifier);
            caster.getAttribute(Attribute.RESISTANCE_MAGICAL).addTransientModifier(modifier);
            addedAt = System.currentTimeMillis();
        }
        return super.onDamage(attacker, damage, type);
    }

    @Override
    protected void onTick() {
        if (addedAt != 0 && System.currentTimeMillis() - addedAt > duration) {
            caster.getAttribute(Attribute.RESISTANCE_PHYSICAL).removeModifier(modifier);
            caster.getAttribute(Attribute.RESISTANCE_MAGICAL).removeModifier(modifier);
            addedAt = 0;
        }
    }
}
