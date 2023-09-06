package de.erethon.spellbook.traits.paladin;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import de.erethon.spellbook.api.TraitTrigger;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;

public class HolyBlessing extends SpellTrait {

    private final double baseHealAmount = data.getDouble("baseHealAmount", 50);
    public HolyBlessing(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onTrigger(TraitTrigger trigger) {
        for (LivingEntity living : trigger.getTargets()) {
            living.setHealth(Math.min(living.getHealth() + baseHealAmount + Spellbook.getScaledValue(data, caster, Attribute.STAT_HEALINGPOWER), living.getMaxHealth()));
        }
    }
}
