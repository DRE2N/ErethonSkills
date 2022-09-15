package de.erethon.spellbook.spells.archer.passive;

import de.erethon.papyrus.DamageType;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.PassiveSpell;
import org.bukkit.entity.LivingEntity;

public class DistantShooter extends PassiveSpell {

    private final double damagePerBlock = data.getDouble("damagePerBlock", 0.5);
    private final int maxDistance = data.getInt("maxDistance", 40);

    public DistantShooter(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    public double onAttack(LivingEntity target, double damage, DamageType type) {
        int distance = (int) Math.min(caster.getLocation().distance(target.getLocation()), maxDistance);
        double bonus = damagePerBlock * distance;
        return super.onAttack(target, damage + bonus, type);
    }
}

