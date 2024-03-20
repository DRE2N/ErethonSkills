package de.erethon.spellbook.traits.ranger;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.entity.LivingEntity;

public class DistantShooter extends SpellTrait {

    private final double damagePerBlock = data.getDouble("damagePerBlock", 0.5);
    private final int maxDistance = data.getInt("maxDistance", 40);

    public DistantShooter(TraitData traitData, LivingEntity caster) {
        super(traitData, caster);
    }

    @Override
    public double onAttack(LivingEntity target, double damage, PDamageType type) {
        int distance = (int) Math.min(caster.getLocation().distance(target.getLocation()), maxDistance);
        double bonus = damagePerBlock * distance;
        return super.onAttack(target, damage + bonus, type);
    }
}

