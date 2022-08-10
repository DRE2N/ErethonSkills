package de.erethon.spellbook.spells.assassin;

import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.EntityTargetSpell;
import de.erethon.spellbook.utils.NMSUtils;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

public class ThrowBola extends EntityTargetSpell {

    AttributeModifier modifier;
    ArmorStand armorStand;

    public ThrowBola(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = data.getInt("duration", 3) * 20;
    }

    @Override
    public boolean onPrecast() {
        return super.onPrecast() && AssassinUtils.hasEnergy(caster, data);
    }

    @Override
    protected boolean onCast() {
        modifier = new AttributeModifier("throwBola-" + caster.getUniqueId(), -100.0, AttributeModifier.Operation.ADD_NUMBER);
        targetEntity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).addModifier(modifier);
        armorStand = targetEntity.getWorld().spawn(targetEntity.getLocation(), ArmorStand.class);
        armorStand.setInvisible(true);
        armorStand.setMarker(true);
        armorStand.getEquipment().setHelmet(new ItemStack(Material.BROWN_CARPET)); // TODO: Model
        NMSUtils.addAttachment(targetEntity, armorStand, -2);
        return true;
    }

    @Override
    protected void onTickFinish() {
        targetEntity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).removeModifier(modifier);
        armorStand.remove();
    }

    @Override
    protected void onAfterCast() {
        caster.removeEnergy(data.getInt("energyCost", 50));
    }
}
