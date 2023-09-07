package de.erethon.spellbook.traits.assassin;

import de.erethon.papyrus.DamageType;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;

import java.util.Random;

public class TwoStepsAhead extends SpellTrait {

    private final int duration = data.getInt("duration", 20);
    private final double missChance = data.getDouble("missChance", 0.5);
    private final Random random = new Random();
    private long lastHit = 0;

    public TwoStepsAhead(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    public double onDamage(LivingEntity attacker, double damage, DamageType type) {
        if (System.currentTimeMillis() - lastHit < duration * 50L && random.nextDouble() < missChance) {
            caster.getWorld().spawnParticle(Particle.CRIT_MAGIC, caster.getLocation(), 3);
            return 0;
        }
        return super.onDamage(attacker, damage, type);
    }

    @Override
    public double onAttack(LivingEntity target, double damage, DamageType type) {
        lastHit = System.currentTimeMillis();
        return super.onAttack(target, damage, type);
    }
}
