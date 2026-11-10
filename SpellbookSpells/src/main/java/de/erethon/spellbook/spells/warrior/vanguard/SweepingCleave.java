package de.erethon.spellbook.spells.warrior.vanguard;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.vfx.SkillSignatureVfx;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import java.util.List;

public class SweepingCleave extends VanguardBaseSpell {

    private static final String SEISMIC_TAG = "vanguard.seismic";
    private static final Color AFTERSHOCK_COLOR = Color.fromRGB(200, 50, 50);

    private final double coneRange = data.getDouble("coneRange", 6.0);
    private final double coneAngle = data.getDouble("coneAngle", 120.0);
    private final double coneHeight = data.getDouble("coneHeight", 3.0);
    private final double minKnockback = data.getDouble("minKnockback", 0.8);
    private final double maxKnockback = data.getDouble("maxKnockback", 1.5);
    private final double aftershockMultiplier = data.getDouble("aftershockMultiplier", 1.4);

    public SweepingCleave(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    public boolean onCast() {
        Vector direction = caster.getLocation().getDirection();
        direction.setY(0);
        if (direction.lengthSquared() == 0) {
            direction.setZ(1);
        } else {
            direction.normalize();
        }

        playWindupEffect();

        createConeAoE(caster.getLocation(), coneRange, coneAngle, coneHeight, direction, 20)
                .onEnter((aoe, entity) -> {
                    if (!entity.equals(caster) && Spellbook.canAttack(caster, entity)) {
                        double physicalDamage = Spellbook.getVariedAttributeBasedDamage(data, caster, entity, true, Attribute.ADVANTAGE_PHYSICAL);

                        if (entity.getTags().contains(SEISMIC_TAG)) {
                            entity.getTags().remove(SEISMIC_TAG);
                            physicalDamage *= aftershockMultiplier;
                            playAftershockEffect(entity);
                        }

                        entity.damage(physicalDamage, caster, PDamageType.PHYSICAL);
                        applyKnockback(entity, direction);
                        playHitEffect(entity);
                    }
                });

        SkillSignatureVfx.accent(caster.getLocation().add(direction.clone().multiply(2)).add(0, 1.4, 0),
                ShieldBreaker.class, "main", 2.0f, 12, false);
        playSwingEffect(direction);

        return super.onCast();
    }

    private void playWindupEffect() {
        caster.getWorld().spawnParticle(Particle.SWEEP_ATTACK, caster.getLocation().add(0, 1, 0), 3, 1.5, 0.5, 1.5, 0);
        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.8f, 0.9f);
    }

    private void playSwingEffect(Vector direction) {
        Vector perpendicular = new Vector(-direction.getZ(), 0, direction.getX()).normalize();
        for (int i = -5; i <= 5; i++) {
            Vector particleOffset = direction.clone().multiply(2).add(perpendicular.clone().multiply(i * 0.4));
            caster.getWorld().spawnParticle(Particle.SWEEP_ATTACK,
                caster.getLocation().add(particleOffset).add(0, 1, 0), 1, 0, 0, 0, 0);
        }
        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 1.0f, 0.8f);
    }

    private void playAftershockEffect(LivingEntity entity) {
        Location loc = entity.getLocation().add(0, 1, 0);
        loc.getWorld().spawnParticle(Particle.EXPLOSION, loc, 1, 0, 0, 0, 0);
        loc.getWorld().spawnParticle(Particle.DUST, loc, 8, 0.4, 0.4, 0.4, 0,
            new Particle.DustOptions(AFTERSHOCK_COLOR, 1.5f));
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.3f);
    }

    private void applyKnockback(LivingEntity target, Vector swingDirection) {
        Vector toTarget = target.getLocation().subtract(caster.getLocation()).toVector().normalize();
        Vector perpendicular = new Vector(-swingDirection.getZ(), 0, swingDirection.getX()).normalize();
        double dotProduct = toTarget.dot(perpendicular);
        Vector knockbackDirection = perpendicular.clone().multiply(dotProduct > 0 ? 1 : -1);
        double knockbackStrength = Spellbook.getRangedValue(data, caster, target, Attribute.ADVANTAGE_PHYSICAL, minKnockback, maxKnockback, "knockbackStrength");
        knockbackDirection.multiply(knockbackStrength);
        knockbackDirection.setY(0.2);
        Vector resultingVelocity = target.getVelocity().add(knockbackDirection);
        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                if (target.isValid() && !target.isDead()) {
                    target.setVelocity(resultingVelocity);
                }
            }
        }.runTaskLater(Spellbook.getInstance().getImplementer(), 1L);
    }

    private void playHitEffect(LivingEntity target) {
        target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 8, 0.3, 0.3, 0.3, 0.1);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.7f, 1.1f);
    }
}
