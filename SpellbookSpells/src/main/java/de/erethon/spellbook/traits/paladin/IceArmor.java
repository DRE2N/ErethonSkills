package de.erethon.spellbook.traits.paladin;

import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;

public class IceArmor extends SpellTrait {

    private final double percent = data.getDouble("percent", 0.5);

    private AttributeModifier modifier;

    public IceArmor(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onAdd() {
        double physRes = caster.getAttribute(Attribute.RES_PHYSICAL).getValue() * percent;
        modifier = new AttributeModifier("IceArmor", physRes, AttributeModifier.Operation.ADD_NUMBER);
        caster.getAttribute(Attribute.RES_MAGIC).addTransientModifier(modifier);
    }

    @Override
    protected void onRemove() {
        caster.getAttribute(Attribute.RES_MAGIC).removeModifier(modifier);
    }

}
