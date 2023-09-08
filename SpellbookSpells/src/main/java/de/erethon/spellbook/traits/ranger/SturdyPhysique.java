package de.erethon.spellbook.traits.ranger;

import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;

public class SturdyPhysique extends SpellTrait {

    private final AttributeModifier modifier = new AttributeModifier("SturdyPhysique", data.getDouble("bonus", 100), AttributeModifier.Operation.ADD_NUMBER);

    public SturdyPhysique(TraitData data, LivingEntity caster) {
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
