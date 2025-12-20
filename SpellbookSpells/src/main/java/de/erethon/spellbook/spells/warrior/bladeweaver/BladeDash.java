package de.erethon.spellbook.spells.warrior.bladeweaver;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

/**
 * Blade Dash - Dash forward and launch up the first enemy hit.
 * Applies Razor Mark to launched enemies.
 * In demon form: Dash through ALL enemies in a line, launching and damaging them all,
 * and gain a brief speed boost afterwards.
 */
public class BladeDash extends BladeweaverBaseSpell {

    private final double dashDistance = data.getDouble("dashDistance", 8.0);
    private final double dashSpeed = data.getDouble("dashSpeed", 2.0);
    private final double launchHeight = data.getDouble("launchHeight", 0.8);
    private final double hitboxWidth = data.getDouble("hitboxWidth", 1.5);
    private final int razorMarkStacks = data.getInt("razorMarkStacks", 1);

    private Vector dashDirection;
    private Location startLocation;
    private boolean dashing = true;
    private final Set<LivingEntity> hitEntities = new HashSet<>();
    private boolean hasHitFirstTarget = false;

    public BladeDash(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = 40; // Max duration to check for hits
    }

    @Override
    public boolean onCast() {
        startLocation = caster.getLocation().clone();
        dashDirection = caster.getLocation().getDirection().setY(0).normalize();
        Vector dashVelocity = dashDirection.clone().multiply(dashSpeed);
        caster.setVelocity(dashVelocity);
        playDashStartEffect();

        return super.onCast();
    }

    @Override
    protected void onTick() {
        super.onTick();
        if (!dashing) return;
        double distanceTraveled = startLocation.distance(caster.getLocation());
        if (distanceTraveled >= dashDistance || isBlockedByWall()) {
            endDash();
            return;
        }
        spawnDashTrail();
        checkHits();
        if (!isInDemonForm() && hasHitFirstTarget) {
            endDash();
        }
    }

    private boolean isBlockedByWall() {
        Vector velocity = caster.getVelocity();
        double horizontalSpeed = Math.sqrt(velocity.getX() * velocity.getX() + velocity.getZ() * velocity.getZ());
        return horizontalSpeed < 0.1 && currentTicks > 2; // Allow a few ticks to build up speed
    }

    private void checkHits() {
        for (LivingEntity entity : caster.getWorld().getNearbyLivingEntities(caster.getLocation(), hitboxWidth)) {
            if (entity.equals(caster) || hitEntities.contains(entity) || !Spellbook.canAttack(caster, entity)) {
                continue;
            }
            hitEntities.add(entity);
            hasHitFirstTarget = true;
            double damage = Spellbook.getVariedAttributeBasedDamage(data, caster, entity, true, Attribute.ADVANTAGE_PHYSICAL);
            entity.damage(damage, caster, PDamageType.PHYSICAL);
            launchTarget(entity);
            addRazorMarkStacks(entity, razorMarkStacks);
            grantBonusHealthForHit();
            playHitEffect(entity);
        }
    }

    private void launchTarget(LivingEntity target) {
        // upward with slight knockback in dash direction
        Vector launch = dashDirection.clone().multiply(0.3);
        launch.setY(launchHeight);
        if (isInDemonForm()) {
            launch.setY(launchHeight * 1.2); // Higher launch in demon form
        }

        target.setVelocity(target.getVelocity().add(launch));
    }

    private void playDashStartEffect() {
        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.3f);
        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 0.5f, 1.8f);

        Particle.DustOptions dust = new Particle.DustOptions(getThemeColor(), 1.5f);
        caster.getWorld().spawnParticle(Particle.DUST, caster.getLocation().add(0, 1, 0), 15, 0.3, 0.5, 0.3, 0, dust);
    }

    private void spawnDashTrail() {
        Location loc = caster.getLocation().add(0, 0.5, 0);
        Particle.DustOptions dust = new Particle.DustOptions(getThemeColor(), 1.0f);

        caster.getWorld().spawnParticle(Particle.DUST, loc, 5, 0.2, 0.3, 0.2, 0, dust);

        if (isInDemonForm()) {
            caster.getWorld().spawnParticle(Particle.FLAME, loc, 2, 0.2, 0.3, 0.2, 0.02);
        }
    }

    private void playHitEffect(LivingEntity target) {
        target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 12, 0.3, 0.3, 0.3, 0.2);
        target.getWorld().spawnParticle(Particle.SWEEP_ATTACK, target.getLocation().add(0, 1, 0), 3, 0.2, 0.2, 0.2, 0);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1.0f, 1.0f);

        Particle.DustOptions goldDust = new Particle.DustOptions(BLADEWEAVER_ACCENT, 1.2f);
        target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(0, 1.5, 0), 8, 0.3, 0.3, 0.3, 0, goldDust);
    }

    private void endDash() {
        dashing = false;

        Vector currentVel = caster.getVelocity();
        caster.setVelocity(new Vector(0, currentVel.getY(), 0));

        Particle.DustOptions dust = new Particle.DustOptions(getThemeColor(), 1.2f);
        caster.getWorld().spawnParticle(Particle.DUST, caster.getLocation().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0, dust);
        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 0.8f, 1.2f);

        if (isInDemonForm() && !hitEntities.isEmpty()) {
            applySpeedBoost();
        }
    }

    private void applySpeedBoost() {
        caster.getWorld().spawnParticle(Particle.END_ROD, caster.getLocation().add(0, 1, 0), 15, 0.3, 0.5, 0.3, 0.1);
        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 0.7f, 1.5f);

        for (int i = 0; i < 3; i++) {
            double angle = Math.random() * Math.PI * 2;
            double x = Math.cos(angle) * 0.5;
            double z = Math.sin(angle) * 0.5;
            caster.getWorld().spawnParticle(Particle.DUST, caster.getLocation().add(x, 0.5, z), 3, 0.1, 0.2, 0.1, 0,
                new Particle.DustOptions(BLADEWEAVER_DEMON, 1.0f));
        }
    }

    @Override
    protected void cleanup() {
        if (dashing) {
            Vector currentVel = caster.getVelocity();
            caster.setVelocity(new Vector(0, currentVel.getY(), 0));
        }
        dashing = false;
        super.cleanup();
    }
}
