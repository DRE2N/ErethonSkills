package de.erethon.spellbook.traits.assassin;

import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;

public class Weakling extends SpellTrait {

    private final AttributeModifier speedModifier = new AttributeModifier("Weakling", data.getDouble("speedBonus"), AttributeModifier.Operation.ADD_NUMBER);
    private final AttributeModifier healthBonus = new AttributeModifier("Weakling", data.getDouble("healthBonus"), AttributeModifier.Operation.ADD_NUMBER);

    public Weakling(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onAdd() {
        caster.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).addTransientModifier(speedModifier);
        caster.getAttribute(Attribute.GENERIC_MAX_HEALTH).addTransientModifier(healthBonus);
    }

    @Override
    protected void onRemove() {
        caster.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).removeModifier(speedModifier);
        caster.getAttribute(Attribute.GENERIC_MAX_HEALTH).removeModifier(healthBonus);
    }
}
