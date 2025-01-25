package de.erethon.spellbook.traits.assassin;

import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlotGroup;

public class ScarredBody extends SpellTrait {

    private final NamespacedKey key = new NamespacedKey("spellbook", "traitscarredbody");
    private final AttributeModifier modifier = new AttributeModifier(key, data.getDouble("bonusHealth", 100), AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY);

    public ScarredBody(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onAdd() {
        caster.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).addTransientModifier(modifier);
    }

    @Override
    protected void onRemove() {
        caster.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).removeModifier(modifier);
    }
}
