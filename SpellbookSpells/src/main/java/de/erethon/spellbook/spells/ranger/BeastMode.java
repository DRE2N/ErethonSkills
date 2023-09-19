package de.erethon.spellbook.spells.ranger;

import de.erethon.spellbook.api.SpellData;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;

public class BeastMode extends RangerPetBaseSpell {

    private final double attributeMultiplier = data.getDouble("attributeMultiplier", 0.2);
    private final AttributeModifier modifier = new AttributeModifier("beastmode", attributeMultiplier, AttributeModifier.Operation.MULTIPLY_SCALAR_1);

    public BeastMode(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = data.getInt("duration", 600);
    }

    @Override
    protected boolean onCast() {
        caster.teleport(pet.getLocation());
        caster.getWorld().playSound(caster.getLocation(), org.bukkit.Sound.ENTITY_WOLF_HOWL, 1, 1);
        pet.remove();
        caster.getAttribute(Attribute.ADV_PHYSICAL).addTransientModifier(modifier);
        caster.getAttribute(Attribute.ADV_MAGIC).addTransientModifier(modifier);
        caster.getAttribute(Attribute.RES_PHYSICAL).addTransientModifier(modifier);
        caster.getAttribute(Attribute.RES_MAGIC).addTransientModifier(modifier);
        caster.getAttribute(Attribute.STAT_HEALTHREGEN).addTransientModifier(modifier);
        caster.getAttribute(Attribute.GENERIC_MAX_HEALTH).addTransientModifier(modifier);
        return true;
    }

    @Override
    protected void onTickFinish() {
        caster.getAttribute(Attribute.ADV_PHYSICAL).removeModifier(modifier);
        caster.getAttribute(Attribute.ADV_MAGIC).removeModifier(modifier);
        caster.getAttribute(Attribute.RES_PHYSICAL).removeModifier(modifier);
        caster.getAttribute(Attribute.RES_MAGIC).removeModifier(modifier);
        caster.getAttribute(Attribute.STAT_HEALTHREGEN).removeModifier(modifier);
        caster.getAttribute(Attribute.GENERIC_MAX_HEALTH).removeModifier(modifier);
        petTrait.spawn();
    }
}
