package de.erethon.spellbook.traits.assassin;

import de.erethon.papyrus.DamageType;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.entity.LivingEntity;

public class DeathBlow extends SpellTrait {

    private final double damageMultiplier = data.getDouble("damageMultiplier", 1.5);

    public DeathBlow(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    public double onAttack(LivingEntity target, double damage, DamageType type) {
        if (target.getHealth() <= target.getMaxHealth() / 2) {
            return damage * damageMultiplier;
        }
        return super.onAttack(target, damage, type);
    }
}
