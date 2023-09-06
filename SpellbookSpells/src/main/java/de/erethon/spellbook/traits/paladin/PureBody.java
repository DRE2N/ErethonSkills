package de.erethon.spellbook.traits.paladin;

import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;

public class PureBody extends SpellTrait {

    private final AttributeModifier healthBuff = new AttributeModifier("PureBodyHealth", data.getDouble("healthBuff", 50), AttributeModifier.Operation.ADD_NUMBER);
    private final AttributeModifier regenBuff = new AttributeModifier("PureBodyRegen", data.getDouble("regenBuff", 2), AttributeModifier.Operation.ADD_NUMBER);

    public PureBody(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onAdd() {
        caster.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).addTransientModifier(healthBuff);
        caster.getAttribute(Attribute.STAT_HEALTHREGEN).addTransientModifier(regenBuff);
    }

    @Override
    protected void onRemove() {
        caster.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).removeModifier(healthBuff);
        caster.getAttribute(Attribute.STAT_HEALTHREGEN).removeModifier(regenBuff);
    }
}
