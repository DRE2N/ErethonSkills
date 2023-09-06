package de.erethon.spellbook.traits.paladin;

import de.erethon.papyrus.DamageType;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;

public class PathOfGod extends SpellTrait {

    private final double healthPercentage = data.getDouble("healthPercentage", 0.5);
    private final AttributeModifier modifier = new AttributeModifier("PathOfGod", data.getDouble("speedBonus", 0.2), AttributeModifier.Operation.ADD_NUMBER);

    public PathOfGod(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    public double onDamage(LivingEntity attacker, double damage, DamageType type) {
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
