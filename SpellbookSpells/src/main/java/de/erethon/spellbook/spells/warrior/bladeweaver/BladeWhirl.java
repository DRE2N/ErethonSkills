package de.erethon.spellbook.spells.warrior.bladeweaver;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.combat.BossControl;
import de.erethon.spellbook.combat.CombatSourceContext;
import de.erethon.spellbook.combat.HealingContext;
import de.erethon.spellbook.spells.vfx.SkillVfx;
import de.erethon.spellbook.spells.vfx.SkillSignatureVfx;
import de.erethon.spellbook.spells.vfx.TerrainRing;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;

import java.util.HashSet;
import java.util.Set;

/**
 * Blade Whirl - Spin attack that damages all nearby enemies.
 * Consumes Razor Mark stacks on hit enemies for bonus damage.
 * In demon form: Creates a persistent whirlwind with orbiting swords that continues damaging enemies.
 */
public class BladeWhirl extends BladeweaverBaseSpell {

    private final double whirlRadius = data.getDouble("whirlRadius", 4.0);
    private final double whirlHeight = data.getDouble("whirlHeight", 2.5);
    private final double bonusDamagePerStack = data.getDouble("bonusDamagePerStack", 0.15);
    private final int demonFormDuration = data.getInt("demonFormDuration", 60); // 3 seconds
    private final int demonFormTickInterval = data.getInt("demonFormTickInterval", 10); // Damage every 0.5s
    private final double baseHealing = data.getDouble("baseHealing", 16);
    private final double healingPowerCoefficient = data.getDouble("healingPowerCoefficient", 0.3);
    private final double healPerMark = data.getDouble("healPerMark", 2);
    private int marksConsumedThisCast;

    private final Set<LivingEntity> hitThisTick = new HashSet<>();
    private int demonWhirlTimer = 0;
    private TerrainRing terrainRing;

    public BladeWhirl(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = 30;
    }

    @Override
    public boolean onCast() {
        if (!super.onCast()) return false;
        if (isInDemonForm()) {
            keepAliveTicks = demonFormDuration;
        }

        if (SkillVfx.availableTo(caster)) {
            terrainRing = new TerrainRing(caster.getLocation(), whirlRadius,
                    "terrain_band_bladeweaver");
        }
        playWhirlEffect();
        playWhirlSound();

        dealWhirlDamage(true);
        double healingPower = Math.min(100, Math.max(0,
                caster.getAttribute(Attribute.STAT_HEALINGPOWER).getValue()));
        double healing = Math.min(caster.getMaxHealth() * 0.3, baseHealing
                + healingPowerCoefficient * healingPower
                + Math.min(5, marksConsumedThisCast) * healPerMark);
        caster.heal(HealingContext.isPlayerCombat(caster) ? healing * 0.65 : healing);

        if (isInDemonForm()) {
            demonWhirlTimer = demonFormDuration;
        }

        return true;
    }

    @Override
    protected void onTick() {
        super.onTick();
        updateFloorRing();
        if (currentTicks % 10 == 0) SkillSignatureVfx.accent(caster.getLocation().add(0, 1, 0),
                getClass(), "main", 3.0f, 14, false);
        if (isInDemonForm() && demonWhirlTimer > 0) {
            demonWhirlTimer--;
            if (demonWhirlTimer % demonFormTickInterval == 0) {
                hitThisTick.clear();
                dealWhirlDamage(false);
            }
            spawnDemonWhirlParticles();
        }

    }

    private void updateFloorRing() {
        if (terrainRing != null) {
            if (currentTicks % 2 == 0) {
                Location center = caster.getLocation();
                terrainRing.move(center);
            }
        }
    }

    private void playWhirlEffect() {
        Location loc = caster.getLocation().add(0, 1, 0);
        Color color = getThemeColor();
        if (SkillVfx.availableTo(caster)) {
            caster.getWorld().spawnParticle(Particle.DUST, loc, 12, 0.7, 0.25, 0.7, 0,
                    new Particle.DustOptions(color, 1.1f));
            caster.getWorld().spawnParticle(Particle.SWEEP_ATTACK, loc, 1);
            return;
        }
        for (int ring = 0; ring < 3; ring++) {
            double radius = (ring + 1) * (whirlRadius / 3);
            int particles = (int) (radius * 8);

            for (int i = 0; i < particles; i++) {
                double angle = (Math.PI * 2 * i / particles);
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;

                Location particleLoc = loc.clone().add(x, 0, z);
                caster.getWorld().spawnParticle(Particle.DUST, particleLoc, 2, 0.1, 0.1, 0.1, 0,
                    new Particle.DustOptions(color, 1.2f));
                caster.getWorld().spawnParticle(Particle.SWEEP_ATTACK, particleLoc, 1, 0, 0, 0, 0);
            }
        }

        caster.getWorld().spawnParticle(Particle.SWEEP_ATTACK, loc, 8, whirlRadius / 2, 0.5, whirlRadius / 2, 0);
    }

    private void playWhirlSound() {
        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.8f);
        caster.getWorld().playSound(caster.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_1, 0.6f, 1.5f);
    }

    private void dealWhirlDamage(boolean consumeMarks) {
        for (LivingEntity entity : caster.getWorld().getNearbyLivingEntities(caster.getLocation(), whirlRadius, whirlHeight)) {
            if (entity.equals(caster) || !Spellbook.canAttack(caster, entity) || hitThisTick.contains(entity)) {
                continue;
            }
            hitThisTick.add(entity);
            double damage = Spellbook.getVariedAttributeBasedDamage(data, caster, entity, true, Attribute.ADVANTAGE_PHYSICAL);
            int stacks = consumeMarks ? getRazorMarkStacks(entity) : 0;
            if (consumeMarks) {
                if (stacks > 0) {
                    double bonusMultiplier = 1 + (stacks * bonusDamagePerStack);
                    damage *= bonusMultiplier;
                }
            }
            double before = entity.getHealth() + entity.getAbsorptionAmount();
            double finalDamage = damage;
            CombatSourceContext.runSpell(this, () -> entity.damage(finalDamage, caster, PDamageType.PHYSICAL));
            if (entity.getHealth() + entity.getAbsorptionAmount() >= before) continue;
            if (consumeMarks && stacks > 0) {
                marksConsumedThisCast += consumeRazorMarkStacks(entity);
            }
            if (consumeMarks) BossControl.apply(caster, entity, stacks > 0 ? 35 : 10);
            playHitEffect(entity);
            grantBonusHealthForHit();
        }
    }

    private void playHitEffect(LivingEntity target) {
        if (currentTicks == 0 || currentTicks % demonFormTickInterval == 0) bladeImpact(target);
        target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 5, 0.2, 0.2, 0.2, 0.1);
    }


    private void spawnDemonWhirlParticles() {
        Location loc = caster.getLocation().add(0, 0.5, 0);
        double time = currentTicks * 0.3;
        for (int i = 0; i < 4; i++) {
            double angle = time + (i * Math.PI / 2);
            double x = Math.cos(angle) * whirlRadius * 0.8;
            double z = Math.sin(angle) * whirlRadius * 0.8;

            Location particleLoc = loc.clone().add(x, 0, z);
            caster.getWorld().spawnParticle(Particle.FLAME, particleLoc, 1, 0.1, 0.2, 0.1, 0.01);
            caster.getWorld().spawnParticle(Particle.DUST, particleLoc, 2, 0.1, 0.1, 0.1, 0,
                new Particle.DustOptions(BLADEWEAVER_DEMON, 1.0f));
        }
    }

    @Override
    protected void cleanup() {
        if (terrainRing != null) {
            terrainRing.close();
            terrainRing = null;
        }
        super.cleanup();
    }
}
