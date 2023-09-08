package de.erethon.spellbook.traits.warrior;

import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import de.erethon.spellbook.api.TraitTrigger;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;

public class SteadyBlow extends SpellTrait {

    private final AttributeModifier bonusModifier = new AttributeModifier("SteadyBlow", data.getDouble("bonus", 20), AttributeModifier.Operation.ADD_NUMBER);
    private final long duration = data.getInt("duration", 100) * 50L;
    private long lastAdded = 0;

    public SteadyBlow(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onTrigger(TraitTrigger trigger) {
        caster.getAttribute(Attribute.RES_PHYSICAL).addTransientModifier(bonusModifier);
        caster.getAttribute(Attribute.RES_MAGIC).addTransientModifier(bonusModifier);
        lastAdded = System.currentTimeMillis();
    }

    @Override
    protected void onTick() {
        if (lastAdded == 0 || System.currentTimeMillis() - lastAdded < duration) return;
        caster.getAttribute(Attribute.RES_PHYSICAL).removeModifier(bonusModifier);
        caster.getAttribute(Attribute.RES_MAGIC).removeModifier(bonusModifier);
        lastAdded = 0;
    }
}
