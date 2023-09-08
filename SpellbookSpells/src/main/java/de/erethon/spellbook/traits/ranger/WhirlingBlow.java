package de.erethon.spellbook.traits.ranger;

import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import de.erethon.spellbook.api.TraitTrigger;
import org.bukkit.entity.LivingEntity;

public class WhirlingBlow extends SpellTrait {

    private final double knockback = data.getDouble("knockback", 1.2);

    public WhirlingBlow(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onTrigger(TraitTrigger trigger) {
        for (LivingEntity living : trigger.getTargets()) {
            living.setVelocity(living.getLocation().toVector().subtract(caster.getLocation().toVector()).normalize().multiply(knockback));
        }
    }
}
