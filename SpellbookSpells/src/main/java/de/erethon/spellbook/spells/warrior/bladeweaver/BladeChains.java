package de.erethon.spellbook.spells.warrior.bladeweaver;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.combat.BossControl;
import de.erethon.spellbook.combat.CombatSourceContext;
import de.erethon.spellbook.spells.vfx.SkillSignatureVfx;
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
                bladeFlash(target.getLocation().add(0, 1, 0), "", 1.7f, 14);
                double damage = Spellbook.getVariedAttributeBasedDamage(data, caster, target, true, Attribute.ADVANTAGE_PHYSICAL, "singleTargetDamage");
                double before = target.getHealth() + target.getAbsorptionAmount();
                CombatSourceContext.runSpell(this, () -> target.damage(damage, caster, PDamageType.PHYSICAL));
                if (target.getHealth() + target.getAbsorptionAmount() < before) {
                    addRazorMarkStacks(target, 1);
                    grantBonusHealthForHit();
                    BossControl.apply(caster, target, 50);
                    target.getWorld().playSound(target.getLocation(), Sound.BLOCK_CHAIN_HIT, 1.0f, 1.2f);
                }
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

        if (tetherTimer % 12 == 0) showTethers();

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
                if (!caster.hasLineOfSight(entity)) continue;
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

        if (distance > 1 && !BossControl.apply(caster, entity, 0)) {
            double strengthMultiplier = Math.min(distance / 5, 2.0);
            Vector pull = toCenter.normalize().multiply(pullStrength * strengthMultiplier);

            pull.setY(Math.max(-0.1, Math.min(0.1, pull.getY())));

            entity.setVelocity(entity.getVelocity().add(pull));
        }
    }

    private void playChainLaunchEffect(Vector direction) {
        Location origin = caster.getEyeLocation();
        bladeFlash(origin.clone().add(direction.clone().multiply(1.5)), "", 2.0f, 14);
        SkillSignatureVfx.accent(origin.clone().add(direction.clone().multiply(chainRange * 0.5)),
                getClass(), "lead", 2.5f, 12, false);

        caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_CHAIN_PLACE, 1.0f, 0.7f);
        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_FISHING_BOBBER_THROW, 0.8f, 0.8f);
    }

    private void playChainHitEffect(LivingEntity target) {
        Location loc = target.getLocation().add(0, 1, 0);
        bladeFlash(loc, "", 1.7f, 16);
        bladeImpact(target);

        Particle.DustOptions dust = new Particle.DustOptions(getThemeColor(), 1.2f);
        target.getWorld().spawnParticle(Particle.DUST, loc, 15, 0.3, 0.5, 0.3, 0, dust);
        target.getWorld().playSound(loc, Sound.BLOCK_CHAIN_HIT, 1.0f, 1.2f);

        double damage = Spellbook.getVariedAttributeBasedDamage(data, caster, target, false, Attribute.ADVANTAGE_PHYSICAL, "chainHitDamage");
        double before = target.getHealth() + target.getAbsorptionAmount();
        CombatSourceContext.runSpell(this, () -> target.damage(damage, caster, PDamageType.PHYSICAL));
        if (target.getHealth() + target.getAbsorptionAmount() < before) {
            BossControl.apply(caster, target, 30);
            grantBonusHealthForHit();
        }
    }

    private void showTethers() {
        Location center = tetherCenter.clone().add(0, 0.9, 0);
        for (LivingEntity entity : tetheredEntities) {
            Location from = entity.getLocation().add(0, 1, 0);
            Vector delta = center.toVector().subtract(from.toVector());
            for (int i = 1; i <= 2; i++) {
                SkillSignatureVfx.accent(from.clone().add(delta.clone().multiply(i / 3.0)),
                        getClass(), "wake", 1.3f, 14, false);
            }
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

