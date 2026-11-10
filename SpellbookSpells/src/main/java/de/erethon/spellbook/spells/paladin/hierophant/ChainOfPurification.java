package de.erethon.spellbook.spells.paladin.hierophant;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.combat.BossControl;
import de.erethon.spellbook.combat.CombatSourceContext;
import de.erethon.spellbook.spells.vfx.SkillVfx;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import java.util.Comparator;

/** A committed luminous chain that controls one enemy or a small empowered cluster. */
public class ChainOfPurification extends HierophantBaseSpell {

    private final double range = data.getDouble("range", 16);
    private final int empoweredCost = data.getInt("minimumEnergyForBonus", 50);
    private final int bonusEnemyCount = data.getInt("bonusEnemyCount", 2);
    private Location chainPoint;
    private Vector direction;
    private boolean empowered;
    private boolean landed;

    public ChainOfPurification(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = (int) Math.ceil(range / 1.2) + 2;
    }

    @Override
    public boolean onCast() {
        empowered = caster.getEnergy() >= empoweredCost;
        if (!super.onCast()) return false;
        if (empowered) caster.setEnergy(Math.max(0, caster.getEnergy() - empoweredCost));
        chainPoint = caster.getEyeLocation().clone();
        direction = chainPoint.getDirection().normalize();
        caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_CHAIN_PLACE, 0.9f,
                empowered ? 0.75f : 1.2f);
        return true;
    }

    @Override
    protected void onTick() {
        super.onTick();
        if (landed || chainPoint == null) return;
        chainPoint.add(direction.clone().multiply(1.2));
        if (!chainPoint.getBlock().isPassable()) {
            landed = true;
            return;
        }
        chainPoint.getWorld().spawnParticle(Particle.END_ROD, chainPoint,
                SkillVfx.availableTo(caster) ? 1 : 5, 0.12, 0.12, 0.12, 0.01);
        var first = chainPoint.getWorld().getNearbyLivingEntities(chainPoint, 1.1).stream()
                .filter(entity -> entity != caster && Spellbook.canAttack(caster, entity)
                        && caster.hasLineOfSight(entity))
                .min(Comparator.comparingDouble(entity -> entity.getLocation().distanceSquared(chainPoint)))
                .orElse(null);
        if (first == null) return;
        landed = true;
        if (!hit(first, 50)) return;
        if (empowered) {
            first.getWorld().getNearbyLivingEntities(first.getLocation(), 3).stream()
                    .filter(entity -> entity != first && entity != caster
                            && Spellbook.canAttack(caster, entity) && caster.hasLineOfSight(entity))
                    .sorted(Comparator.comparingDouble(entity -> entity.getLocation().distanceSquared(first.getLocation())))
                    .limit(bonusEnemyCount)
                    .forEach(entity -> hit(entity, 20));
        }
    }

    private boolean hit(LivingEntity enemy, double control) {
        double damage = Spellbook.getVariedAttributeBasedDamage(data, caster, enemy, true,
                Attribute.ADVANTAGE_MAGICAL);
        double before = enemy.getHealth() + enemy.getAbsorptionAmount();
        CombatSourceContext.runSpell(this, () -> enemy.damage(damage, caster, PDamageType.MAGIC));
        if (enemy.getHealth() + enemy.getAbsorptionAmount() >= before) return false;
        if (!BossControl.apply(caster, enemy, control)) {
            Vector pull = caster.getLocation().toVector().subtract(enemy.getLocation().toVector()).setY(0);
            if (pull.lengthSquared() > 0.01) {
                enemy.setVelocity(enemy.getVelocity().add(pull.normalize().multiply(0.4)));
            }
        }
        enemy.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, enemy.getEyeLocation(),
                SkillVfx.availableTo(caster) ? 2 : 10, 0.25, 0.3, 0.25, 0.04);
        enemy.getWorld().playSound(enemy.getLocation(), Sound.BLOCK_CHAIN_HIT, 0.9f, 1.0f);
        triggerTraits(enemy);
        return true;
    }
}
