package de.erethon.spellbook.effects.ranger;
import de.erethon.spellbook.utils.TargetedParticles;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellEffect;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;

import java.util.Random;

public class MarkOfTheHuntedEffect extends SpellEffect {

    private final double damageMultiplier = data.getDouble("damageMultiplier", 1.15);
    private final Random random = new Random();

    public MarkOfTheHuntedEffect(EffectData data, LivingEntity caster, LivingEntity target, int duration, int stacks) {
        super(data, caster, target, duration, stacks);
    }

    @Override
    public void tick() {
        TargetedParticles.spawnForOthers(Particle.HAPPY_VILLAGER, target,
                target.getLocation().add(random.nextDouble(-0.5, 0.5), 1.9, random.nextDouble(-0.5, 0.5)), 1, 0, 0, 0);

    }

    @Override
    public void onApply() {
        target.setGlowing(true);
    }

    @Override
    public void onRemove() {
        target.setGlowing(false);
    }

    @Override
    public double onDamage(LivingEntity damager, double damage, PDamageType type) {
        if (damager == caster) {
            return damage * damageMultiplier;
        }
        return super.onDamage(damager, damage, type);
    }
}
