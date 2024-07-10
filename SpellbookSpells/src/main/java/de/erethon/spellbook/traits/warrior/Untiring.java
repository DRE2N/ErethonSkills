package de.erethon.spellbook.traits.warrior;

import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlotGroup;

public class Untiring extends SpellTrait {

    private final double healthRegenPerMissingHealthPercent = data.getDouble("healthRegenPerMissingHealthPercent", 0.1);
    private AttributeModifier modifier = null;
    private final NamespacedKey key = new NamespacedKey("spellbook", "traituntiring");

    public Untiring(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onTick() {
        double missingHealthPercent = 1 - (caster.getHealth() / caster.getMaxHealth());
        if (missingHealthPercent == 0) {
            return;
        }
        double healthRegen = missingHealthPercent * healthRegenPerMissingHealthPercent;
        if (modifier != null) {
            caster.getAttribute(Attribute.STAT_HEALTHREGEN).removeModifier(modifier);
        }
        modifier = new AttributeModifier(key, healthRegen, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY);
        caster.getAttribute(Attribute.STAT_HEALTHREGEN).addTransientModifier(modifier);
        super.onTick();
    }
}
