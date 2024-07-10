package de.erethon.spellbook.traits.assassin;

import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlotGroup;

public class ForTheGoal extends SpellTrait {

    private final NamespacedKey key = new NamespacedKey("spellbook", "traitforthegoal");
    private final AttributeModifier modifier = new AttributeModifier(key, data.getDouble("damageBonus", 5.0), AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY);

    public ForTheGoal(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onAdd() {
        caster.getAttribute(Attribute.ADV_PHYSICAL).addTransientModifier(modifier);
    }

    @Override
    protected void onRemove() {
        caster.getAttribute(Attribute.ADV_PHYSICAL).removeModifier(modifier);
    }
}
