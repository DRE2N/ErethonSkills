package de.erethon.spellbook.traits.paladin;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import de.erethon.spellbook.api.TraitTrigger;
import org.bukkit.entity.LivingEntity;

public class ThickShield extends SpellTrait {
    private final double range = data.getDouble("range", 5);
    private final double behindMultiplier = data.getDouble("behindMultiplier", -2);
    private final int duration = data.getInt("duration", 40);
    private final EffectData effectData = Spellbook.getEffectData("ThickShieldEffect");

    public ThickShield(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onTrigger(TraitTrigger trigger) {
        for (LivingEntity living : caster.getLocation().getDirection().multiply(behindMultiplier).toLocation(caster.getWorld()).getNearbyLivingEntities(range)) {
            if (living == caster || Spellbook.canAttack(caster, living)) continue;
            living.addEffect(caster, effectData, duration, 1);
        }
    }

}
