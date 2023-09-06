package de.erethon.spellbook.traits.paladin;

import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;

public class HierophantTraining extends SpellTrait {

    private final AttributeModifier modifier = new AttributeModifier("HierophantTraining", data.getDouble("bonus", 20), AttributeModifier.Operation.ADD_NUMBER);

    public HierophantTraining(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onAdd() {
        caster.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).addTransientModifier(modifier);
    }

    @Override
    protected void onRemove() {
        caster.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).removeModifier(modifier);
    }
}
