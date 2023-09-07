package de.erethon.spellbook.traits.assassin;

import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;

public class ScarredBody extends SpellTrait {

    private final AttributeModifier modifier = new AttributeModifier("ScarredBody", data.getDouble("bonusHealth", 100), AttributeModifier.Operation.ADD_NUMBER);

    public ScarredBody(TraitData data, LivingEntity caster) {
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
