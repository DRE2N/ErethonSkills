package de.erethon.spellbook.spells.warrior.bladeweaver;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

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

        float yaw = (float) Math.atan2(direction.getX(), direction.getZ());

        swordDisplay = currentLocation.getWorld().spawn(currentLocation, ItemDisplay.class, display -> {
            display.setItemStack(new ItemStack(Material.GOLDEN_SWORD));
            display.setBillboard(Display.Billboard.FIXED);
            display.setGlowing(true);
            display.setPersistent(false);
            display.setGlowColorOverride(getThemeColor());
            display.setInterpolationDuration(2);
            display.setInterpolationDelay(0);
            updateSwordTransformation(display, yaw, 0);
        });

        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.2f);

        return super.onCast();
    }

    private void updateSwordTransformation(ItemDisplay display, float yaw, float spin) {
        Quaternionf rotation = new Quaternionf();
        rotation.rotateY(yaw);
        rotation.rotateX((float) Math.PI / 2);
        rotation.rotateZ(spin);

        display.setTransformation(new Transformation(
            new Vector3f(0, 0, 0),
            rotation,
            new Vector3f(1.5f, 1.5f, 1.5f),
            new Quaternionf()
        ));
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
        swordDisplay.teleport(currentLocation);
        distanceTraveled += projectileSpeed;

        spinAngle += 0.5f; // Spin speed
        float yaw = (float) Math.atan2(moveDirection.getX(), moveDirection.getZ());
        updateSwordTransformation(swordDisplay, yaw, spinAngle);

        spawnTrailParticles();
        checkHits();

        if (!returning && distanceTraveled >= maxDistance) {
            if (isInDemonForm()) {
                returning = true;
                distanceTraveled = 0;
                hitEntities.clear(); // Can hit enemies again on return
                caster.getWorld().playSound(currentLocation, Sound.ITEM_TRIDENT_RETURN, 1.0f, 1.5f);

                Particle.DustOptions dust = new Particle.DustOptions(BLADEWEAVER_DEMON, 1.5f);
                currentLocation.getWorld().spawnParticle(Particle.DUST, currentLocation, 20, 0.5, 0.5, 0.5, 0, dust);
            } else {
                cleanup();
            }
        }
        if (returning && currentLocation.distance(caster.getEyeLocation()) < 1.5) {
            cleanup();
        }
    }

    private void spawnTrailParticles() {
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
            entity.damage(damage, caster, PDamageType.PHYSICAL);
            addRazorMarkStacks(entity, razorMarkStacksPerHit);
            grantBonusHealthForHit();

            playHitEffect(entity);
        }
    }

    private void playHitEffect(LivingEntity target) {
        target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.1);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.8f, 1.0f);
        Particle.DustOptions goldDust = new Particle.DustOptions(BLADEWEAVER_ACCENT, 1.2f);
        target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(0, 1.5, 0), 8, 0.3, 0.3, 0.3, 0, goldDust);
    }

    @Override
    protected void cleanup() {
        if (swordDisplay != null && swordDisplay.isValid()) {
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 * i / 8);
                double x = Math.cos(angle) * 0.5;
                double z = Math.sin(angle) * 0.5;
                Location loc = currentLocation.clone().add(x, 0, z);
                currentLocation.getWorld().spawnParticle(Particle.DUST, loc, 3, 0.1, 0.1, 0.1, 0,
                    new Particle.DustOptions(getThemeColor(), 1.2f));
            }
            currentLocation.getWorld().spawnParticle(Particle.SWEEP_ATTACK, currentLocation, 3, 0.2, 0.2, 0.2, 0);
            swordDisplay.remove();
        }
        super.cleanup();
    }
}

