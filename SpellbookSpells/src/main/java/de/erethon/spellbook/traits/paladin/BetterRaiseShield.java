package de.erethon.spellbook.traits.paladin;

import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import de.erethon.spellbook.api.TraitTrigger;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;

public class BetterRaiseShield extends SpellTrait {

    private final AttributeModifier modifier = new AttributeModifier("BetterRaiseShield", data.getDouble("bonus", 20), AttributeModifier.Operation.ADD_NUMBER);

    public BetterRaiseShield(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onTrigger(TraitTrigger trigger) {
        if (trigger.getId() == 0) {
            caster.getAttribute(Attribute.RES_MAGIC).addTransientModifier(modifier);
            caster.getAttribute(Attribute.RES_PHYSICAL).addTransientModifier(modifier);
        }
        if (trigger.getId() == 1) {
            caster.getAttribute(Attribute.RES_MAGIC).removeModifier(modifier);
            caster.getAttribute(Attribute.RES_PHYSICAL).removeModifier(modifier);
        }
    }
}
