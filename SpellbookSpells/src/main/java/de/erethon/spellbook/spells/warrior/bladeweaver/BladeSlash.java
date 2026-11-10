package de.erethon.spellbook.spells.warrior.bladeweaver;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.combat.BossControl;
import de.erethon.spellbook.combat.CombatSourceContext;
import de.erethon.spellbook.spells.vfx.SkillVfx;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

/**
 * Blade Slash - Throws a spectral sword that pierces through enemies.
 * Each enemy hit applies a Razor Mark stack.
 * In demon form: The blade returns like a boomerang, hitting enemies twice.
 */
public class BladeSlash extends BladeweaverBaseSpell {

    private final double projectileSpeed = data.getDouble("projectileSpeed", 1.5);
    private final double maxDistance = data.getDouble("maxDistance", 20.0);
    private final double hitboxWidth = data.getDouble("hitboxWidth", 1.0);
    private final int razorMarkStacksPerHit = data.getInt("razorMarkStacksPerHit", 1);

    private ItemDisplay swordDisplay;
    private Vector direction;
    private Location currentLocation;
    private double distanceTraveled = 0;
    private final Set<LivingEntity> hitEntities = new HashSet<>();
    private boolean returning = false;
    private float spinAngle = 0;

    public BladeSlash(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = 100; // Max 5 seconds
    }

    @Override
    public boolean onCast() {
        direction = caster.getLocation().getDirection().normalize();
        currentLocation = caster.getEyeLocation().clone();

        swordDisplay = trackVfx(SkillVfx.projectile(currentLocation, direction,
                "sig_warrior_bladeweaver_bladeslash_f4", 1.8f));

        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.2f);

        return super.onCast();
    }

    @Override
    protected void onTick() {
        super.onTick();

        if (swordDisplay == null || !swordDisplay.isValid()) {
            cleanup();
            return;
        }

        Vector moveDirection = returning ?
            caster.getEyeLocation().toVector().subtract(currentLocation.toVector()).normalize() :
            direction.clone();

        currentLocation.add(moveDirection.clone().multiply(projectileSpeed));
        if (!currentLocation.getBlock().isPassable()) {
            cleanup();
            return;
        }
        swordDisplay.teleport(currentLocation);
        SkillVfx.orientProjectile(swordDisplay, moveDirection);
        distanceTraveled += projectileSpeed;

        spinAngle += 0.5f;

        spawnTrailParticles();
        checkHits();

        if (!returning && distanceTraveled >= maxDistance) {
            if (isInDemonForm()) {
                returning = true;
                distanceTraveled = 0;
                hitEntities.clear(); // Can hit enemies again on return
                caster.getWorld().playSound(currentLocation, Sound.ITEM_TRIDENT_RETURN, 1.0f, 1.5f);
                bladeFlash(currentLocation, "", 1.5f, 12);

                Particle.DustOptions dust = new Particle.DustOptions(BLADEWEAVER_DEMON, 1.5f);
                currentLocation.getWorld().spawnParticle(Particle.DUST, currentLocation,
                        4, 0.4, 0.4, 0.4, 0, dust);
            } else {
                cleanup();
            }
        }
        if (returning && currentLocation.distance(caster.getEyeLocation()) < 1.5) {
            cleanup();
        }
    }

    private void spawnTrailParticles() {
        if (currentTicks % 3 != 0) return;
        Particle.DustOptions dust = new Particle.DustOptions(getThemeColor(), 1.0f);
        currentLocation.getWorld().spawnParticle(Particle.DUST, currentLocation, 3, 0.1, 0.1, 0.1, 0, dust);

        double trailAngle = spinAngle * 2;
        for (int i = 0; i < 2; i++) {
            double offsetAngle = trailAngle + (i * Math.PI);
            double xOff = Math.cos(offsetAngle) * 0.3;
            double yOff = Math.sin(offsetAngle) * 0.3;
            Location trailLoc = currentLocation.clone().add(xOff, yOff, 0);
            currentLocation.getWorld().spawnParticle(Particle.DUST, trailLoc, 1, 0, 0, 0, 0, dust);
        }

        if (isInDemonForm()) {
            currentLocation.getWorld().spawnParticle(Particle.FLAME, currentLocation, 2, 0.1, 0.1, 0.1, 0.02);
        }
    }

    private void checkHits() {
        for (LivingEntity entity : currentLocation.getWorld().getNearbyLivingEntities(currentLocation, hitboxWidth)) {
            if (entity.equals(caster) || hitEntities.contains(entity) || !Spellbook.canAttack(caster, entity)) {
                continue;
            }

            hitEntities.add(entity);
            double damage = Spellbook.getVariedAttributeBasedDamage(data, caster, entity, true, Attribute.ADVANTAGE_PHYSICAL);
            double before = entity.getHealth() + entity.getAbsorptionAmount();
            CombatSourceContext.runSpell(this, () -> entity.damage(damage, caster, PDamageType.PHYSICAL));
            if (entity.getHealth() + entity.getAbsorptionAmount() >= before) continue;
            addRazorMarkStacks(entity, razorMarkStacksPerHit);
            grantBonusHealthForHit();
            BossControl.apply(caster, entity, 15);

            playHitEffect(entity);
        }
    }

    private void playHitEffect(LivingEntity target) {
        bladeImpact(target);
        target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0),
                3, 0.3, 0.3, 0.3, 0.1);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.8f, 1.0f);
        Particle.DustOptions goldDust = new Particle.DustOptions(BLADEWEAVER_ACCENT, 1.2f);
        target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(0, 1.5, 0),
                2, 0.3, 0.3, 0.3, 0, goldDust);
    }

    @Override
    protected void cleanup() {
        if (swordDisplay != null && swordDisplay.isValid()) swordDisplay.remove();
        super.cleanup();
    }
}

