package de.erethon.spellbook.traits.paladin;

import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;

public class MeatShield extends SpellTrait {

    private final AttributeModifier modifier = new AttributeModifier("MeatShield", data.getDouble("bonus", 50), AttributeModifier.Operation.ADD_NUMBER);

    public MeatShield(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onAdd() {
        caster.getAttribute(Attribute.GENERIC_MAX_HEALTH).addTransientModifier(modifier);
    }

    @Override
    protected void onRemove() {
        caster.getAttribute(Attribute.GENERIC_MAX_HEALTH).removeModifier(modifier);
    }
}
