package de.erethon.spellbook.traits.warrior;

import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;

public class MasterOfSteel extends SpellTrait {

    private final double conversionMultiplier = data.getDouble("conversionMultiplier", 0.33);
    private AttributeModifier modifier = null;

    public MasterOfSteel(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    // Have to do this on tick as other things might affect stats
    @Override
    protected void onTick() {
        double advPhysical = caster.getAttribute(Attribute.ADV_PHYSICAL).getValue();
        if (modifier != null) {
            caster.getAttribute(Attribute.RES_PHYSICAL).removeModifier(modifier);
        }
        modifier = new AttributeModifier("MasterOfSteel", advPhysical * conversionMultiplier, AttributeModifier.Operation.ADD_NUMBER);
        caster.getAttribute(Attribute.RES_PHYSICAL).addTransientModifier(modifier);
    }
}
