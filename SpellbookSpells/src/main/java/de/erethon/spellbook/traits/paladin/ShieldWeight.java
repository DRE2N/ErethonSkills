package de.erethon.spellbook.traits.paladin;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import de.erethon.spellbook.api.TraitTrigger;
import org.bukkit.entity.LivingEntity;

public class ShieldWeight extends SpellTrait {

    private final double range = data.getDouble("range", 3);
    private final double power = data.getDouble("power", 1);

    public ShieldWeight(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onTrigger(TraitTrigger trigger) {
        for (LivingEntity living : trigger.getTarget().getLocation().getNearbyLivingEntities(range)) {
            if (!Spellbook.canAttack(caster, living)) continue;
            living.setVelocity(caster.getLocation().getDirection().multiply(power));
        }
    }
}
