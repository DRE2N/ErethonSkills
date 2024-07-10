package de.erethon.spellbook.traits.warrior;

import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlotGroup;

public class MasterOfSteel extends SpellTrait {

    private final NamespacedKey key = new NamespacedKey("spellbook", "traitmasterofsteel");
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
        modifier = new AttributeModifier(key, advPhysical * conversionMultiplier, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY);
        caster.getAttribute(Attribute.RES_PHYSICAL).addTransientModifier(modifier);
    }
}
