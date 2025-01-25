package de.erethon.spellbook.traits.warrior;

import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import de.erethon.spellbook.api.TraitTrigger;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlotGroup;

public class SteadyBlow extends SpellTrait {

    private final NamespacedKey key = new NamespacedKey("spellbook", "traitsteadyblow");
    private final AttributeModifier bonusModifier = new AttributeModifier(key, data.getDouble("bonus", 20), AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY);
    private final long duration = data.getInt("duration", 100) * 50L;
    private long lastAdded = 0;

    public SteadyBlow(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onTrigger(TraitTrigger trigger) {
        caster.getAttribute(Attribute.RESISTANCE_PHYSICAL).addTransientModifier(bonusModifier);
        caster.getAttribute(Attribute.RESISTANCE_MAGICAL).addTransientModifier(bonusModifier);
        lastAdded = System.currentTimeMillis();
    }

    @Override
    protected void onTick() {
        if (lastAdded == 0 || System.currentTimeMillis() - lastAdded < duration) return;
        caster.getAttribute(Attribute.RESISTANCE_PHYSICAL).removeModifier(bonusModifier);
        caster.getAttribute(Attribute.RESISTANCE_MAGICAL).removeModifier(bonusModifier);
        lastAdded = 0;
    }
}
