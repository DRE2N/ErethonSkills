package de.erethon.spellbook.traits.archer;

import de.erethon.papyrus.DamageType;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.entity.LivingEntity;

public class HighGround extends SpellTrait {

    private final double damageMultiplier = data.getDouble("damageMultiplier", 1.05);
    private final int minDistance = data.getInt("minDistance", 5);

    public HighGround(TraitData traitData, LivingEntity caster) {
        super(traitData, caster);
    }

    @Override
    public double onAttack(LivingEntity target, double damage, DamageType type) {
        if ((target.getLocation().getY() + minDistance) < caster.getLocation().getY()) {
            return damage * damageMultiplier;
        }
        return super.onAttack(target, damage, type);
    }
}