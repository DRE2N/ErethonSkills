package de.erethon.spellbook.traits.assassin;

import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;

public class LightLuggage extends SpellTrait {

    private final AttributeModifier modifier = new AttributeModifier("LightLuggage", data.getDouble("speedBonus"), AttributeModifier.Operation.ADD_NUMBER);
    private final AttributeModifier defenseModifier = new AttributeModifier("LightLuggageDefense", data.getDouble("defenseMultiplier"), AttributeModifier.Operation.ADD_SCALAR);

    public LightLuggage(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onAdd() {
        caster.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).addTransientModifier(modifier);
        caster.getAttribute(Attribute.RES_PHYSICAL).addTransientModifier(defenseModifier);
        caster.getAttribute(Attribute.RES_MAGIC).addTransientModifier(defenseModifier);
    }

    @Override
    protected void onRemove() {
        caster.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).removeModifier(modifier);
        caster.getAttribute(Attribute.RES_PHYSICAL).removeModifier(defenseModifier);
        caster.getAttribute(Attribute.RES_MAGIC).removeModifier(defenseModifier);
    }
}
