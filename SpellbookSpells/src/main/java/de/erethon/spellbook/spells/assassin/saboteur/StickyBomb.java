package de.erethon.spellbook.spells.assassin.saboteur;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.assassin.AssassinBaseSpell;
import de.slikey.effectlib.Effect;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.SphereEffect;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class StickyBomb extends AssassinBaseSpell {

    // Throws a sticky bomb at the target, dealing damage and applying a vulnerability effect.

    private final int range = data.getInt("range", 15);
    private final int delayTicks = data.getInt("delay", 60);
    private final double explosionRadius = data.getDouble("radius", 4.0);
    private final double bonusDamageMultiplier = data.getDouble("bonusDamageMultiplier", 1.5);
    private final EffectData vulnerabilityEffectData = Bukkit.getServer().getSpellbookAPI().getLibrary().getEffectByID("Weakness");
    private Location lastTargetLocation;
    private Effect attachedBombEffect;

    public StickyBomb(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onPrecast() {
        if (!super.onPrecast()) {
            return false;
        }
        return lookForTarget(range);
    }

    @Override
    public boolean onCast() {
        keepAliveTicks = delayTicks;
        tickInterval = 5;
        caster.getLocation().getWorld().playSound(caster.getLocation(), Sound.ENTITY_SNOWBALL_THROW, 1.0f, 1.2f);
        attachVisualEffectToTarget();
        return super.onCast();
    }

    @Override
    protected void onTick() {
        if (target == null || !target.isValid()) {
            if (lastTargetLocation == null) {
                interrupt();
            }
        } else {
            lastTargetLocation = target.getLocation();
        }
    }

    @Override
    protected void onTickFinish() {
        cleanupAttachedEffect();

        Location explosionCenter = (target != null && target.isValid()) ? target.getEyeLocation() : lastTargetLocation;
        if (explosionCenter == null) {
            return;
        }

        playExplosionVisualEffect(explosionCenter);
        explosionCenter.getWorld().playSound(explosionCenter, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 1.0f);

        Set<LivingEntity> affectedTargets = new HashSet<>();
        Collection<LivingEntity> nearbyEntities = explosionCenter.getWorld().getNearbyLivingEntities(explosionCenter, explosionRadius);

        for (Entity entity : nearbyEntities) {
            if (entity instanceof LivingEntity hitTarget && !entity.equals(caster) && Spellbook.canAttack(caster, hitTarget)) {
                double damage = Spellbook.getVariedAttributeBasedDamage(data, caster, hitTarget, false, Attribute.ADVANTAGE_MAGICAL);

                if (hitTarget.equals(target) && target.hasEffect(vulnerabilityEffectData)) {
                    damage *= bonusDamageMultiplier;
                }

                hitTarget.damage(damage, caster);
                affectedTargets.add(hitTarget);
            }
        }

        if (!affectedTargets.isEmpty()) {
            triggerTraits(affectedTargets);
        }
    }

    @Override
    protected void cleanup() {
        super.cleanup();
        cleanupAttachedEffect();
        this.target = null;
        this.lastTargetLocation = null;
    }

    private void attachVisualEffectToTarget() {
        EffectManager effectManager = Spellbook.getInstance().getEffectManager();
        if (effectManager == null || target == null) return;

        SphereEffect effect = new SphereEffect(effectManager);
        effect.setEntity(caster);
        effect.setTargetEntity(target);
        effect.particle = Particle.SMOKE;
        effect.particles = 5;
        effect.duration = delayTicks + 10;
        effect.period = 4;
        effect.particleCount = 1;
        effect.particleOffsetX = 0.2f;
        effect.particleOffsetY = 0.2f;
        effect.particleOffsetZ = 0.2f;
        effect.particleData = 0f;
        effect.start();

        this.attachedBombEffect = effect;
    }

    private void cleanupAttachedEffect() {
        if (attachedBombEffect != null && !attachedBombEffect.isDone()) {
            attachedBombEffect.cancel();
            attachedBombEffect = null;
        }
    }

    private void playExplosionVisualEffect(Location center) {
        EffectManager effectManager = Spellbook.getInstance().getEffectManager();
        if (effectManager == null) return;

        SphereEffect effect = new SphereEffect(effectManager);
        effect.setLocation(center);
        effect.radius = (float) explosionRadius;
        effect.particles = (int) (explosionRadius * explosionRadius * 15);
        effect.particle = Particle.EXPLOSION;
        effect.duration = 10;
        effect.particleCount = 1;
        effect.start();

        SphereEffect smoke = new SphereEffect(effectManager);
        smoke.setLocation(center);
        smoke.radius = (float) explosionRadius * 0.8f;
        smoke.particles = (int) (explosionRadius * explosionRadius * 10);
        smoke.particle = Particle.SMOKE;
        smoke.duration = 20;
        smoke.particleCount = 1;
        smoke.start();
    }
}