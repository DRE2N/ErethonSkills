package de.erethon.spellbook.spells.warrior.butcher;

import de.erethon.spellbook.api.SpellData;
import org.bukkit.entity.LivingEntity;

public class ShoutPull extends AbstractWarriorShout {

    public ShoutPull(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onPrecast() {
        return lookForTarget(range);
    }

    @Override
    public boolean onCast() {
        double distance = caster.getLocation().distance(target.getLocation());
        target.setVelocity(caster.getLocation().getDirection().multiply(-1).multiply(data.getDouble("strength", 1)).multiply(distance / 10));
        triggerTraits(target);
        return super.onCast();
    }
}
