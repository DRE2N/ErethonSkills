package de.erethon.spellbook.spells.paladin.hierophant;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.combat.BossControl;
import de.erethon.spellbook.combat.CombatSourceContext;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import java.util.Comparator;
import java.util.List;
import org.bukkit.entity.ItemDisplay;

/** One heavy, warned glaive lane. Wrath makes the landed blow radiant. */
public class HierophantsVerdict extends HierophantBaseSpell {

    private final double range = data.getDouble("range", 5.5);
    private final double halfWidth = data.getDouble("halfWidth", 0.9);
    private final int empoweredCost = data.getInt("empoweredCost", 40);

    public HierophantsVerdict(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    public boolean onCast() {
        boolean empowered = caster.getEnergy() >= empoweredCost;
        if (!super.onCast()) return false;
        if (empowered) caster.setEnergy(Math.max(0, caster.getEnergy() - empoweredCost));
        Location origin = caster.getLocation().clone();
        Vector direction = origin.getDirection().setY(0);
        if (direction.lengthSquared() < 0.01) direction.setZ(1);
        direction.normalize();
        for (int i = 1; i <= 6; i++) {
            Location point = origin.clone().add(direction.clone().multiply(range * i / 6));
            origin.getWorld().spawnParticle(Particle.END_ROD, point.add(0, 0.25, 0),
                    empowered ? 6 : 3, halfWidth * 0.45, 0.1, halfWidth * 0.45);
        }
        origin.getWorld().playSound(origin, Sound.BLOCK_ANVIL_PLACE, 0.75f, 0.6f);
        Vector committedDirection = direction.clone();
        Bukkit.getScheduler().runTaskLater(Spellbook.getInstance().getImplementer(),
                () -> strike(origin, committedDirection, empowered), 8L);
        return true;
    }

    private void strike(Location origin, Vector direction, boolean empowered) {
        if (!caster.isValid() || caster.isDead() || caster.getWorld() != origin.getWorld()
                || caster.getLocation().distanceSquared(origin) > 9) return;
        var enemy = origin.getWorld().getNearbyLivingEntities(origin, range, 2.5, range).stream()
                .filter(entity -> Spellbook.canAttack(caster, entity) && caster.hasLineOfSight(entity))
                .filter(entity -> {
                    Vector offset = entity.getLocation().toVector().subtract(origin.toVector()).setY(0);
                    double forward = offset.dot(direction);
                    double lateral = offset.clone().subtract(direction.clone().multiply(forward)).length();
                    return forward > 0 && forward <= range && lateral <= halfWidth;
                })
                .min(Comparator.comparingDouble(entity -> entity.getLocation().distanceSquared(origin)))
                .orElse(null);
        if (enemy == null) return;
        Attribute attribute = empowered ? Attribute.ADVANTAGE_MAGICAL : Attribute.ADVANTAGE_PHYSICAL;
        double damage = Spellbook.getVariedAttributeBasedDamage(data, caster, enemy, true,
                attribute, empowered ? "empoweredDamage" : "normalDamage");
        double before = enemy.getHealth() + enemy.getAbsorptionAmount();
        CombatSourceContext.runSpell(this, () -> enemy.damage(damage, caster,
                empowered ? PDamageType.MAGIC : PDamageType.PHYSICAL));
        if (enemy.getHealth() + enemy.getAbsorptionAmount() >= before) return;
        BossControl.apply(caster, enemy, empowered ? 35 : 25);
        if (empowered) enemy.addEffect(caster, Spellbook.getEffectData("Weakness"), 40, 1);
        enemy.getWorld().playSound(enemy.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 1.0f, 0.65f);
        enemy.getWorld().spawnParticle(Particle.FLASH, enemy.getEyeLocation(), 1);
        triggerTraits(enemy);
    }
}
