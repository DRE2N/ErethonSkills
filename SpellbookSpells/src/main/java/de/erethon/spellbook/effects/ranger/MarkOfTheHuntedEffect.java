package de.erethon.spellbook.effects.ranger;

import de.erethon.papyrus.DamageType;
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
        target.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, target.getLocation().add(random.nextDouble(-0.5, 0.5), 1.9,random.nextDouble(-0.5, 0.5)), 1);

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
    public double onDamage(LivingEntity damager, double damage, DamageType type) {
        if (damager == caster) {
            return damage * damageMultiplier;
        }
        return super.onDamage(damager, damage, type);
    }
}
