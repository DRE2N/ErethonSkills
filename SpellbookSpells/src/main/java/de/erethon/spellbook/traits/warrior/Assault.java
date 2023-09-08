package de.erethon.spellbook.traits.warrior;

import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;

public class Assault extends SpellTrait {

    private final AttributeModifier speedModifier = new AttributeModifier("AssaultSpeed", data.getDouble("bonusSpeed", 0.1), AttributeModifier.Operation.ADD_NUMBER);
    private final AttributeModifier physicalModifier = new AttributeModifier("AssaultPhysical", data.getDouble("bonusPhysical", 25), AttributeModifier.Operation.ADD_NUMBER);

    public Assault(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onAdd() {
        caster.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).addTransientModifier(speedModifier);
        caster.getAttribute(Attribute.ADV_PHYSICAL).addTransientModifier(physicalModifier);
    }

    @Override
    protected void onRemove() {
        caster.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).removeModifier(speedModifier);
        caster.getAttribute(Attribute.ADV_PHYSICAL).removeModifier(physicalModifier);
    }
}
