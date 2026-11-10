package de.erethon.spellbook.spells.paladin.inquisitor;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.combat.BossControl;
import de.erethon.spellbook.combat.CombatSourceContext;
import de.erethon.spellbook.combat.HealingContext;
import de.erethon.spellbook.spells.vfx.SkillSignatureVfx;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import java.util.List;

/** A shown spear cone that spends Judgement for one bounded rescue and burn. */
public class Condemn extends InquisitorBaseSpell {

    private final double range = data.getDouble("range", 5);
    private final double halfAngleCosine = Math.cos(Math.toRadians(data.getDouble("coneAngle", 80) / 2));
    private final double baseHealing = data.getDouble("baseHealing", 12);
    private final double healingPowerCoefficient = data.getDouble("healingPowerCoefficient", 0.3);
    private final double healPerJudgement = data.getDouble("healPerJudgement", 2);
    private final double playerCombatMultiplier = data.getDouble("playerCombatMultiplier", 0.65);
    private final EffectData burning = Spellbook.getEffectData("Burning");

    public Condemn(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    public boolean onCast() {
        if (!super.onCast()) return false;
        Location origin = caster.getLocation();
        Vector direction = origin.getDirection().setY(0);
        if (direction.lengthSquared() < 0.01) direction.setZ(1);
        direction.normalize();
        showSpearLane(origin, direction);
        Vector committedDirection = direction.clone();
        Bukkit.getScheduler().runTaskLater(Spellbook.getInstance().getImplementer(), () -> {
            if (caster.isDead() || !caster.isValid() || caster.getWorld() != origin.getWorld()) return;
            int totalJudgement = strikeCone(origin, committedDirection);
            healRescue(totalJudgement);
            SkillSignatureVfx.accent(origin, getClass(), "wake", (float) range, 18, true);
        }, 6L);
        return true;
    }

    private int strikeCone(Location origin, Vector direction) {
        int spent = 0;
        int hits = 0;
        for (LivingEntity target : caster.getWorld().getNearbyLivingEntities(origin, range, 2.5, range)) {
            if (target == caster || !Spellbook.canAttack(caster, target) || !caster.hasLineOfSight(target)) continue;
            Vector toward = target.getLocation().toVector().subtract(origin.toVector()).setY(0);
            if (toward.lengthSquared() > range * range || toward.lengthSquared() < 0.01
                    || direction.dot(toward.normalize()) < halfAngleCosine) continue;
            double damage = Spellbook.getVariedAttributeBasedDamage(data, caster, target,
                    true, Attribute.ADVANTAGE_MAGICAL);
            double before = target.getHealth() + target.getAbsorptionAmount();
            CombatSourceContext.runSpell(this, () -> target.damage(damage, caster, PDamageType.MAGIC));
            if (target.getHealth() + target.getAbsorptionAmount() >= before) continue;
            int marks = Math.min(5, getJudgementStacksOnTarget(target));
            if (marks > 0) {
                removeJudgement(target);
                target.addEffect(caster, burning, 60, target instanceof Player ? Math.min(2, marks) : marks);
                spent += marks;
            }
            BossControl.apply(caster, target, marks > 0 ? 40 : 15);
            SkillSignatureVfx.accent(target.getLocation().add(0, 1, 0), getClass(),
                    marks > 0 ? "main" : "wake", 2.2f, 14, false);
            hits++;
            if (hits >= 5) break;
        }
        caster.getWorld().playSound(caster.getLocation(), Sound.ITEM_TRIDENT_HIT, 0.9f, 0.8f);
        return Math.min(5, spent);
    }

    private void healRescue(int marks) {
        double healingPower = Math.min(100, Math.max(0,
                caster.getAttribute(Attribute.STAT_HEALINGPOWER).getValue()));
        double healing = baseHealing + healingPower * healingPowerCoefficient + marks * healPerJudgement;
        healOne(caster, healing);
        if (caster.getTargetEntity(6, true) instanceof LivingEntity ally && ally != caster
                && !Spellbook.canAttack(caster, ally) && caster.hasLineOfSight(ally)) {
            healOne(ally, healing);
        }
        caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 0.65f, 1.3f);
    }

    private void healOne(LivingEntity target, double healing) {
        double capped = Math.min(target.getMaxHealth() * 0.25, healing);
        target.heal(HealingContext.isPlayerCombat(caster, target)
                ? capped * playerCombatMultiplier : capped);
        SkillSignatureVfx.accent(target.getLocation().add(0, 1.2, 0), getClass(),
                "lead", 1.7f, 15, false);
    }

    private void showSpearLane(Location origin, Vector direction) {
        Vector side = new Vector(-direction.getZ(), 0, direction.getX());
        for (int step = 1; step <= 5; step++) {
            double distance = step * range / 5;
            double width = 0.4 + distance * 0.5;
            for (int edge : new int[]{-1, 1}) {
                Location point = origin.clone().add(direction.clone().multiply(distance))
                        .add(side.clone().multiply(width * edge)).add(0, 0.15, 0);
                caster.getWorld().spawnParticle(Particle.DUST, point, 2, 0.08, 0.02, 0.08, 0,
                        new Particle.DustOptions(org.bukkit.Color.fromRGB(205, 54, 75), 1.0f));
            }
        }
        caster.getWorld().playSound(origin, Sound.ITEM_TRIDENT_THROW, 0.8f, 0.7f);
    }
}
