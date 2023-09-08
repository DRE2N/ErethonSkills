package de.erethon.spellbook.traits.ranger;

import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;

public class LightFeet extends SpellTrait {

    private final AttributeModifier modifier = new AttributeModifier("LightFeet", data.getDouble("bonus", 0.2), AttributeModifier.Operation.ADD_NUMBER);

    public LightFeet(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    public void onAdd() {
        caster.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).addTransientModifier(modifier);
    }

    @Override
    public void onRemove() {
        caster.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).removeModifier(modifier);
    }
}
