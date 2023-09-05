package de.erethon.spellbook.traits.paladin;

import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;

public class IncreasedRegeneration extends SpellTrait {

    private final AttributeModifier regenBonus = new AttributeModifier("IncreasedRegeneration", data.getDouble("bonus", 0.5), AttributeModifier.Operation.ADD_NUMBER);

    public IncreasedRegeneration(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onAdd() {
        caster.getAttribute(Attribute.STAT_HEALTHREGEN).addTransientModifier(regenBonus);
    }

    @Override
    protected void onRemove() {
        caster.getAttribute(Attribute.STAT_HEALTHREGEN).removeModifier(regenBonus);
    }
}
