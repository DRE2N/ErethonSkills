package de.erethon.spellbook.traits.assassin;

import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class PowerfulJump extends SpellTrait {

    private final NamespacedKey key = new NamespacedKey("spellbook", "traitpowerfuljump");
    private final AttributeModifier modifier = new AttributeModifier(key, data.getDouble("jumpEffectStrength", 1), AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY);

    public PowerfulJump(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onAdd() {
        caster.getAttribute(Attribute.JUMP_STRENGTH).addTransientModifier(modifier);
    }

    @Override
    protected void onRemove() {
        caster.getAttribute(Attribute.JUMP_STRENGTH).removeModifier(modifier);
    }
}
