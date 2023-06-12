package de.erethon.spellbook.spells.ranger;

import de.erethon.spellbook.api.SpellData;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;

public class BeastMode extends RangerPetBaseSpell {

    private final double attributeMultiplier = data.getDouble("attributeMultiplier", 1.2);
    private final AttributeModifier modifier = new AttributeModifier("beastmode", attributeMultiplier, AttributeModifier.Operation.MULTIPLY_SCALAR_1);

    public BeastMode(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = data.getInt("duration", 600);
    }

    @Override
    protected boolean onCast() {
        caster.teleport(pet.getLocation());
        pet.remove();
        for (Attribute attribute : Attribute.values()) {
            if (caster.getAttribute(attribute) == null) continue;
            caster.getAttribute(attribute).addTransientModifier(modifier);
        }
        return true;
    }

    @Override
    protected void onTickFinish() {
        for (Attribute attribute : Attribute.values()) {
            if (caster.getAttribute(attribute) == null) continue;
            caster.getAttribute(attribute).removeModifier(modifier);
        }
        petTrait.spawn();
    }
}
