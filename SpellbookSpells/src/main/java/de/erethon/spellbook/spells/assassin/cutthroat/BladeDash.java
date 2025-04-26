package de.erethon.spellbook.spells.assassin.cutthroat;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.assassin.AssassinBaseSpell;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.LineEffect;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

public class BladeDash extends AssassinBaseSpell {

    private final double dashDistance = data.getDouble("distance", 6.0);
    private final double dashSpeedMultiplier = data.getDouble("speedMultiplier", 1.8);
    private final double damageWidth = data.getDouble("damageWidth", 1.5);

    public BladeDash(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    public boolean onCast() {
        if (!super.onCast()) {
            return false;
        }

        Location startLocation = caster.getLocation();
        Vector direction = startLocation.getDirection().normalize();
        Vector dashVector = direction.clone().multiply(dashDistance);
        Location endLocation = startLocation.clone().add(dashVector);

        Set<LivingEntity> affectedTargets = new HashSet<>();
        BoundingBox dashPathBox = BoundingBox.of(startLocation, endLocation).expand(damageWidth / 2.0, 0.5, damageWidth / 2.0);

        for (Entity entity : caster.getWorld().getNearbyEntities(dashPathBox)) {
            if (entity instanceof LivingEntity && !entity.equals(caster) && Spellbook.canAttack(caster, target)) {
                double damage = Spellbook.getVariedAttributeBasedDamage(data, caster, target, true, Attribute.ADVANTAGE_PHYSICAL);
                target.damage(damage, caster);
                affectedTargets.add(target);
                playHitEffect(target.getEyeLocation());
            }
        }

        Vector velocity = direction.multiply(dashSpeedMultiplier);
        caster.setVelocity(velocity);

        playVisualEffect(startLocation, direction);
        playSoundEffect(startLocation);
        triggerTraits(affectedTargets);

        return true;
    }

    private void playVisualEffect(Location start, Vector direction) {
        EffectManager effectManager = Spellbook.getInstance().getEffectManager();
        if (effectManager == null) return;

        LineEffect effect = new LineEffect(effectManager);
        effect.setLocation(start.clone().add(0, 0.5, 0));
        effect.setTarget(start.clone().add(direction.clone().multiply(dashDistance)).add(0, 0.5, 0));
        effect.particle = Particle.CRIT;
        effect.particles = (int) (dashDistance * 5);
        effect.duration = 8;
        effect.start();

        LineEffect trailEffect = new LineEffect(effectManager);
        trailEffect.setLocation(start.clone().add(0, 0.5, 0));
        trailEffect.setTarget(start.clone().add(direction.clone().multiply(dashDistance)).add(0, 0.5, 0));
        trailEffect.particle = Particle.DUST;
        trailEffect.particles = (int) (dashDistance * 3);
        trailEffect.duration = 10;
        trailEffect.start();
    }

    private void playHitEffect(Location hitLocation) {
        hitLocation.getWorld().spawnParticle(Particle.CRIT, hitLocation, 5, 0.2, 0.2, 0.2, 0.1);
    }

    private void playSoundEffect(Location location) {
        location.getWorld().playSound(location, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.5f);
        location.getWorld().playSound(location, Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 1.8f);
    }
}