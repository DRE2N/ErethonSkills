package de.erethon.spellbook.traits.paladin;

import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import de.erethon.spellbook.api.TraitTrigger;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlotGroup;

public class BetterRaiseShield extends SpellTrait {

    private final NamespacedKey key = new NamespacedKey("spellbook", "traitbetterraiseshield");
    private final AttributeModifier modifier = new AttributeModifier(key, data.getDouble("bonus", 20), AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY);

    public BetterRaiseShield(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onTrigger(TraitTrigger trigger) {
        if (trigger.getId() == 0) {
            caster.getAttribute(Attribute.RESISTANCE_PHYSICAL).addTransientModifier(modifier);
            caster.getAttribute(Attribute.RESISTANCE_MAGICAL).addTransientModifier(modifier);
        }
        if (trigger.getId() == 1) {
            caster.getAttribute(Attribute.RESISTANCE_PHYSICAL).removeModifier(modifier);
            caster.getAttribute(Attribute.RESISTANCE_MAGICAL).removeModifier(modifier);
        }
    }
}
