package de.erethon.spellbook.traits.paladin;

import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlotGroup;

public class IronSkin extends SpellTrait {

    private final NamespacedKey key = new NamespacedKey("spellbook", "traitironskin");
    private final AttributeModifier bonus = new AttributeModifier(key, 2, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY);

    public IronSkin(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onAdd() {
        caster.getAttribute(Attribute.MAX_HEALTH).addTransientModifier(bonus);
    }

    @Override
    protected void onRemove() {
        caster.getAttribute(Attribute.MAX_HEALTH).removeModifier(bonus);
    }
}
