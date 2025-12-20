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

import java.util.ArrayList;
import java.util.List;

/**
 * Blade Chains - Throw chains in a cone that tether enemies together.
 * Tethered enemies are pulled towards each other over time.
 * In demon form: Chains also apply Razor Marks and deal damage over time.
 */
public class BladeChains extends BladeweaverBaseSpell {

    private final double chainRange = data.getDouble("chainRange", 12.0);
    private final double chainAngle = data.getDouble("chainAngle", 60.0);
    private final int tetherDuration = data.getInt("tetherDuration", 60); // 3 seconds
    private final double pullStrength = data.getDouble("pullStrength", 0.15);
    private final double chainBreakDistance = data.getDouble("chainBreakDistance", 15.0);
    private final int demonDotInterval = data.getInt("demonDotInterval", 10); // DoT every 0.5s

    private final List<LivingEntity> tetheredEntities = new ArrayList<>();
    private Location tetherCenter;
    private int tetherTimer = 0;
    private boolean tethersActive = false;

    public BladeChains(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = tetherDuration + 20;
    }

    @Override
    public boolean onCast() {
        // Find targets in cone
        Vector direction = caster.getLocation().getDirection();
        findTargetsInCone(direction);

        if (tetheredEntities.size() < 2) {
            if (tetheredEntities.size() == 1) {
                LivingEntity target = tetheredEntities.get(0);
                double damage = Spellbook.getVariedAttributeBasedDamage(data, caster, target, true, Attribute.ADVANTAGE_PHYSICAL, "singleTargetDamage");
                target.damage(damage, caster, PDamageType.PHYSICAL);
                addRazorMarkStacks(target, 1);
                playChainHitEffect(target);
            }
            tetheredEntities.clear();
            return super.onCast();
        }

        updateTetherCenter();
        tethersActive = true;
        tetherTimer = tetherDuration;

        playChainLaunchEffect(direction);
        for (LivingEntity entity : tetheredEntities) {
            playChainHitEffect(entity);

            if (isInDemonForm()) {
                addRazorMarkStacks(entity, 1);
            }
        }

        return super.onCast();
    }

    @Override
    protected void onTick() {
        super.onTick();

        if (!tethersActive || tetherTimer <= 0) return;

        tetherTimer--;

        tetheredEntities.removeIf(entity ->
            !entity.isValid() ||
            entity.isDead() ||
            entity.getLocation().distance(tetherCenter) > chainBreakDistance
        );

        if (tetheredEntities.size() < 2) {
            endTethers();
            return;
        }

        updateTetherCenter();

        for (LivingEntity entity : tetheredEntities) {
            pullTowardsCenter(entity);
        }

        spawnChainParticles();

        if (isInDemonForm() && tetherTimer % demonDotInterval == 0) {
            applyDemonDot();
        }

        if (tetherTimer <= 0) {
            endTethers();
        }
    }

    private void findTargetsInCone(Vector direction) {
        Location origin = caster.getEyeLocation();
        double halfAngle = Math.toRadians(chainAngle / 2);

        for (LivingEntity entity : caster.getWorld().getNearbyLivingEntities(origin, chainRange)) {
            if (entity.equals(caster) || !Spellbook.canAttack(caster, entity)) {
                continue;
            }
            Vector toEntity = entity.getLocation().toVector().subtract(origin.toVector()).normalize();
            double angle = direction.angle(toEntity);

            if (angle <= halfAngle) {
                tetheredEntities.add(entity);
            }
        }
    }

    private void updateTetherCenter() {
        if (tetheredEntities.isEmpty()) return;

        double x = 0, y = 0, z = 0;
        for (LivingEntity entity : tetheredEntities) {
            Location loc = entity.getLocation();
            x += loc.getX();
            y += loc.getY();
            z += loc.getZ();
        }

        int count = tetheredEntities.size();
        tetherCenter = new Location(caster.getWorld(), x / count, y / count, z / count);
    }

    private void pullTowardsCenter(LivingEntity entity) {
        Vector toCenter = tetherCenter.toVector().subtract(entity.getLocation().toVector());
        double distance = toCenter.length();

        if (distance > 1) {
            double strengthMultiplier = Math.min(distance / 5, 2.0);
            Vector pull = toCenter.normalize().multiply(pullStrength * strengthMultiplier);

            pull.setY(Math.max(-0.1, Math.min(0.1, pull.getY())));

            entity.setVelocity(entity.getVelocity().add(pull));
        }
    }

    private void playChainLaunchEffect(Vector direction) {
        Location origin = caster.getEyeLocation();

        for (int i = 0; i < 10; i++) {
            Vector offset = direction.clone().multiply(i * 0.5);
            Location loc = origin.clone().add(offset);

            Particle.DustOptions dust = new Particle.DustOptions(getThemeColor(), 1.5f);
            caster.getWorld().spawnParticle(Particle.DUST, loc, 3, 0.2, 0.2, 0.2, 0, dust);
        }

        caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_CHAIN_PLACE, 1.0f, 0.7f);
        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_FISHING_BOBBER_THROW, 0.8f, 0.8f);
    }

    private void playChainHitEffect(LivingEntity target) {
        Location loc = target.getLocation().add(0, 1, 0);

        Particle.DustOptions dust = new Particle.DustOptions(getThemeColor(), 1.2f);
        target.getWorld().spawnParticle(Particle.DUST, loc, 15, 0.3, 0.5, 0.3, 0, dust);
        target.getWorld().playSound(loc, Sound.BLOCK_CHAIN_HIT, 1.0f, 1.2f);

        double damage = Spellbook.getVariedAttributeBasedDamage(data, caster, target, false, Attribute.ADVANTAGE_PHYSICAL, "chainHitDamage");
        target.damage(damage, caster, PDamageType.PHYSICAL);

        grantBonusHealthForHit();
    }

    private void spawnChainParticles() {
        if (tetheredEntities.size() < 2) return;
        for (int i = 0; i < tetheredEntities.size(); i++) {
            for (int j = i + 1; j < tetheredEntities.size(); j++) {
                drawChainBetween(tetheredEntities.get(i), tetheredEntities.get(j));
            }
        }
        Particle.DustOptions centerDust = new Particle.DustOptions(BLADEWEAVER_ACCENT, 1.0f);
        tetherCenter.getWorld().spawnParticle(Particle.DUST, tetherCenter.clone().add(0, 1, 0), 3, 0.2, 0.2, 0.2, 0, centerDust);
    }

    private void drawChainBetween(LivingEntity a, LivingEntity b) {
        Location locA = a.getLocation().add(0, 1, 0);
        Location locB = b.getLocation().add(0, 1, 0);

        Vector direction = locB.toVector().subtract(locA.toVector());
        double distance = direction.length();
        direction.normalize();

        Particle.DustOptions dust = new Particle.DustOptions(getThemeColor(), 0.8f);

        int particles = (int) Math.ceil(distance * 2);
        for (int i = 0; i <= particles; i++) {
            double progress = (double) i / particles;
            Location point = locA.clone().add(direction.clone().multiply(distance * progress));

            double wave = Math.sin(progress * Math.PI * 2 + currentTicks * 0.2) * 0.1;
            point.add(0, wave, 0);

            a.getWorld().spawnParticle(Particle.DUST, point, 1, 0, 0, 0, 0, dust);
        }
    }

    private void applyDemonDot() {
        for (LivingEntity entity : tetheredEntities) {
            double damage = Spellbook.getVariedAttributeBasedDamage(data, caster, entity, false, Attribute.ADVANTAGE_PHYSICAL, "dotDamage");
            entity.damage(damage, caster, PDamageType.PHYSICAL);
            entity.getWorld().spawnParticle(Particle.FLAME, entity.getLocation().add(0, 1, 0), 3, 0.2, 0.3, 0.2, 0.01);
        }
    }

    private void endTethers() {
        tethersActive = false;

        if (!tetheredEntities.isEmpty()) {
            for (LivingEntity entity : tetheredEntities) {
                Location loc = entity.getLocation().add(0, 1, 0);
                entity.getWorld().spawnParticle(Particle.DUST, loc, 10, 0.3, 0.3, 0.3, 0,
                    new Particle.DustOptions(getThemeColor(), 1.0f));
            }

            caster.getWorld().playSound(tetherCenter, Sound.BLOCK_CHAIN_BREAK, 1.0f, 1.0f);
        }

        tetheredEntities.clear();
    }

    @Override
    protected void cleanup() {
        endTethers();
        super.cleanup();
    }
}

