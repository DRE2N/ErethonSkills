package de.erethon.spellbook.traits.assassin;

import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlotGroup;

public class ShadowWalker extends SpellTrait {

    private final NamespacedKey key = new NamespacedKey("spellbook", "traitshadowwalker");
    private final int lightLevel = data.getInt("lightLevel", 7);
    private final AttributeModifier modifier = new AttributeModifier(key, data.getDouble("speedBonus"), AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY);

    public ShadowWalker(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onTick() {
        int light = caster.getLocation().add(0, -1, 0).getBlock().getLightLevel();
        if (light > lightLevel) {
            caster.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MOVEMENT_SPEED).removeModifier(modifier);
        } else {
            caster.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MOVEMENT_SPEED).addTransientModifier(modifier);
        }

    }
}
