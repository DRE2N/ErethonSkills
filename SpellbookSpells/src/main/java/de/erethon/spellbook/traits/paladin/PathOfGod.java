package de.erethon.spellbook.traits.paladin;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlotGroup;

public class PathOfGod extends SpellTrait {

    private final NamespacedKey key = new NamespacedKey("spellbook", "traitpathofgod");
    private final double healthPercentage = data.getDouble("healthPercentage", 0.5);
    private final AttributeModifier modifier = new AttributeModifier(key, data.getDouble("speedBonus", 0.2), AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY);

    public PathOfGod(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    public double onDamage(LivingEntity attacker, double damage, PDamageType type) {
        if (caster.getHealth() / caster.getMaxHealth() < healthPercentage && !caster.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getModifiers().contains(modifier)) {
            caster.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).addTransientModifier(modifier);
        }
        return super.onDamage(attacker, damage, type);
    }

    @Override
    protected void onTick() {
        if (caster.getHealth() / caster.getMaxHealth() > healthPercentage) {
            caster.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).removeModifier(modifier);
        }
    }
}
