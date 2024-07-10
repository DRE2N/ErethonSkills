package de.erethon.spellbook.traits.assassin;

import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlotGroup;

public class Relentless extends SpellTrait {

    private final NamespacedKey key = new NamespacedKey("spellbook", "traitrelentless");
    private final AttributeModifier damageBonus = new AttributeModifier(key, data.getDouble("damageBonus"), AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY);

    public Relentless(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onAdd() {
        caster.getAttribute(Attribute.ADV_PHYSICAL).addTransientModifier(damageBonus);
    }

    @Override
    protected void onRemove() {
        caster.getAttribute(Attribute.ADV_PHYSICAL).removeModifier(damageBonus);
    }
}
