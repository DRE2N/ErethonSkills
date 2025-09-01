package de.erethon.spellbook.spells.warrior.vanguard;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.aoe.AoE;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.warrior.WarriorBaseSpell;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

public class SweepingCleave extends WarriorBaseSpell {

    // Perform a wide, horizontal swing, striking all enemies in a cone in front of you.
    // Enemies hit are slightly knocked back sideways.

    private final double coneRange = data.getDouble("coneRange", 6.0);
    private final double coneAngle = data.getDouble("coneAngle", 120.0);
    private final double coneHeight = data.getDouble("coneHeight", 3.0);
    private final double minKnockback = data.getDouble("minKnockback", 0.8);
    private final double maxKnockback = data.getDouble("maxKnockback", 1.5);

    public SweepingCleave(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    public boolean onCast() {
        Vector direction = caster.getLocation().getDirection();

        playWindupEffect();

        createConeAoE(caster.getLocation(), coneRange, coneAngle, coneHeight, direction, 20)
                .onEnter((aoe, entity) -> {
                    if (!entity.equals(caster) && Spellbook.canAttack(caster, entity)) {
                        double physicalDamage = Spellbook.getVariedAttributeBasedDamage(data, caster, entity, true, Attribute.ADVANTAGE_PHYSICAL);
                        entity.damage(physicalDamage, caster, PDamageType.PHYSICAL);

                        applyKnockback(entity, direction);
                        playHitEffect(entity);
                    }
                });

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

    private void applyKnockback(LivingEntity target, Vector swingDirection) {
        Vector toTarget = target.getLocation().subtract(caster.getLocation()).toVector().normalize();
        Vector perpendicular = new Vector(-swingDirection.getZ(), 0, swingDirection.getX()).normalize();

        double dotProduct = toTarget.dot(perpendicular);
        Vector knockbackDirection = perpendicular.clone().multiply(dotProduct > 0 ? 1 : -1);

        double knockbackStrength = Spellbook.getRangedValue(data, caster, target, Attribute.ADVANTAGE_PHYSICAL, minKnockback, maxKnockback, "knockbackStrength");

        knockbackDirection.multiply(knockbackStrength);
        knockbackDirection.setY(0.2);

        target.setVelocity(target.getVelocity().add(knockbackDirection));
    }

    private void playHitEffect(LivingEntity target) {
        target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 8, 0.3, 0.3, 0.3, 0.1);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.7f, 1.1f);
    }
}
