package de.erethon.spellbook.spells.warrior.swordstorm;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.combat.BossControl;
import de.erethon.spellbook.combat.CombatSourceContext;
import de.erethon.spellbook.spells.vfx.SkillSignatureVfx;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Comparator;

/** A shown grounded sword lane that rewards an earned sprint approach. */
public class ChargingSlash extends SwordstormBaseSpell {

    private final double range = data.getDouble("range", 4.5);
    private final double laneHalfWidth = data.getDouble("laneHalfWidth", 0.8);

    public ChargingSlash(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    public boolean onCast() {
        if (!super.onCast()) return false;
        Location origin = caster.getLocation();
        Vector direction = origin.getDirection().setY(0);
        if (direction.lengthSquared() < 0.01) direction.setZ(1);
        direction.normalize();
        boolean sprinting = caster instanceof Player player && player.isSprinting();
        showLane(origin, direction, sprinting);
        Vector committedDirection = direction.clone();
        Bukkit.getScheduler().runTaskLater(Spellbook.getInstance().getImplementer(), () ->
                strikeLane(origin, committedDirection, sprinting), 4L);
        return true;
    }

    private void showLane(Location origin, Vector direction, boolean sprinting) {
        SkillSignatureVfx.accent(origin.clone().add(direction.clone().multiply(range * 0.5)),
                getClass(), "lead", (float) range, 8, true);
        Vector side = new Vector(-direction.getZ(), 0, direction.getX());
        for (int step = 1; step <= 5; step++) {
            double distance = step * range / 5;
            for (int edge : new int[]{-1, 1}) {
                Location point = origin.clone().add(direction.clone().multiply(distance))
                        .add(side.clone().multiply(laneHalfWidth * edge)).add(0, 0.1, 0);
                caster.getWorld().spawnParticle(Particle.DUST, point, 2, 0.07, 0.02, 0.07, 0,
                        new Particle.DustOptions(sprinting ? org.bukkit.Color.MAROON
                                : org.bukkit.Color.PURPLE, 0.9f));
            }
        }
        caster.getWorld().playSound(origin, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.65f, 0.75f);
    }

    private void strikeLane(Location origin, Vector direction, boolean sprinting) {
        if (!caster.isValid() || caster.isDead() || caster.getWorld() != origin.getWorld()
                || caster.getLocation().distanceSquared(origin) > 9) return;
        var target = caster.getWorld().getNearbyLivingEntities(origin, range, 2.5, range).stream()
                .filter(entity -> entity != caster && Spellbook.canAttack(caster, entity))
                .filter(entity -> {
                    Vector offset = entity.getLocation().toVector().subtract(origin.toVector()).setY(0);
                    double forward = offset.dot(direction);
                    double side = offset.clone().subtract(direction.clone().multiply(forward)).length();
                    return forward > 0 && forward <= range && side <= laneHalfWidth
                            && caster.hasLineOfSight(entity);
                })
                .min(Comparator.comparingDouble(entity -> entity.getLocation().distanceSquared(origin)))
                .orElse(null);
        if (target == null) return;
        double damage = Spellbook.getVariedAttributeBasedDamage(data, caster, target, true,
                Attribute.ADVANTAGE_PHYSICAL, sprinting ? "sprintingDamage" : "normalDamage");
        double before = target.getHealth() + target.getAbsorptionAmount();
        CombatSourceContext.runSpell(this, () -> target.damage(damage, caster, PDamageType.PHYSICAL));
        if (target.getHealth() + target.getAbsorptionAmount() >= before) return;
        BossControl.apply(caster, target, sprinting ? 35 : 25);
        Vector push = direction.clone().multiply(0.22);
        target.setVelocity(target.getVelocity().add(push));
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG,
                sprinting ? 1.0f : 0.75f, sprinting ? 0.85f : 1.15f);
        target.getWorld().spawnParticle(Particle.SWEEP_ATTACK, target.getEyeLocation(), 2,
                0.2, 0.2, 0.2, 0.02);
        SkillSignatureVfx.accent(target.getEyeLocation(), getClass(),
                sprinting ? "main" : "wake", sprinting ? 1.35f : 1.1f, 7, false);
        caster.swingMainHand();
    }
}
