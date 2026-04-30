package de.erethon.spellbook.effects.ranger;

import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellEffect;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;

import java.util.Random;

public class DruidSeedEffect extends SpellEffect {

    private final Random random = new Random();

    public DruidSeedEffect(EffectData data, LivingEntity caster, LivingEntity target, int duration, int stacks) {
        super(data, caster, target, duration, stacks);
    }

    @Override
    public void onTick() {
        if (ticksLeft % 12 != 0) {
            return;
        }
        target.getWorld().spawnParticle(Particle.DUST,
            target.getLocation().add(random.nextDouble(-0.35, 0.35), 1.2 + random.nextDouble(0.45), random.nextDouble(-0.35, 0.35)),
            Math.max(1, stacks), 0.08, 0.08, 0.08, 0,
            new Particle.DustOptions(Color.fromRGB(102, 190, 86), 0.8f));
    }
}
