package de.erethon.spellbook.traits.assassin;

import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;

public class PiercingBlade extends SpellTrait {

    private final AttributeModifier modifier = new AttributeModifier("PiercingBlade", data.getDouble("bonusPhysPenetration", 15), AttributeModifier.Operation.ADD_NUMBER);

    public PiercingBlade(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onAdd() {
        caster.getAttribute(Attribute.PEN_PHYSICAL).addTransientModifier(modifier);
    }

    @Override
    protected void onRemove() {
        caster.getAttribute(Attribute.PEN_PHYSICAL).removeModifier(modifier);
    }
}
