package de.erethon.spellbook.traits.warrior;

import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;

public class GoodCondition extends SpellTrait {

    private final AttributeModifier modifier = new AttributeModifier("GoodCondition", data.getDouble("healthBonus", 100), AttributeModifier.Operation.ADD_NUMBER);

    public GoodCondition(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onAdd() {
        caster.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).addTransientModifier(modifier);
    }

    @Override
    protected void onRemove() {
        caster.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).removeModifier(modifier);
    }
}
