package de.erethon.spellbook.spells.assassin;

import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.EntityTargetSpell;
import de.erethon.spellbook.utils.AssassinUtils;
import de.erethon.spellbook.utils.NMSUtils;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

public class ThrowBola extends AssassinBaseSpell {

    AttributeModifier modifier;
    ArmorStand armorStand;

    public ThrowBola(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = data.getInt("duration", 3) * 20;
    }

    @Override
    public boolean onPrecast() {
        return super.onPrecast() && lookForTarget();
    }

    @Override
    protected boolean onCast() {
        modifier = new AttributeModifier("throwBola-" + caster.getUniqueId(), -100.0, AttributeModifier.Operation.ADD_NUMBER);
        target.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).addModifier(modifier);
        triggerTraits(target);
        armorStand = target.getWorld().spawn(target.getLocation(), ArmorStand.class);
        armorStand.setInvisible(true);
        armorStand.setMarker(true);
        armorStand.getEquipment().setHelmet(new ItemStack(Material.BROWN_CARPET)); // TODO: Model
        NMSUtils.addAttachment(target, armorStand, -2);
        return true;
    }

    @Override
    protected void cleanup() {
        target.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).removeModifier(modifier);
        armorStand.remove();
    }

}
