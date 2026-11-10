package de.erethon.spellbook.spells.paladin.hierophant;
import de.erethon.spellbook.utils.TargetedParticles;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.aoe.AoE;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.combat.CombatSourceContext;
import de.erethon.spellbook.combat.HealingContext;
import de.erethon.spellbook.spells.paladin.PaladinBaseSpell;

import de.erethon.spellbook.spells.vfx.SkillVfx;
import de.erethon.spellbook.spells.vfx.TerrainRing;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventPriority;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class CelestialBarrier extends HierophantBaseSpell implements Listener {

    // The Hierophant creates a celestial barrier of light that absorbs damage for a short duration.
    // When the barrier expires or breaks, it explodes, dealing damage to nearby enemies equal to 100% of the damage absorbed.
    // If cast while you have more than 50% wrath, the barrier reflects 150% damage and creates sacred ground that heals allies.

    private final double radius = data.getDouble("radius", 5.0);
    private final double explosionRadius = data.getDouble("explosionRadius", 7.0);
    private final int minWrathForReflect = data.getInt("minWrathForBonusReflect", 50);
    private final double reflectDamageMultiplier = data.getDouble("reflectDamageMultiplier", 1.0f);
    private final double bonusReflectDamageMultiplier = data.getDouble("bonusReflectDamageMultiplier", 1.5f);
    private final double baseHealing = data.getDouble("baseHealing", 14);
    private final double healingPowerCoefficient = data.getDouble("healingPowerCoefficient", 0.3);
    private final double baseAbsorb = data.getDouble("baseAbsorb", 24);
    private final double resistanceCoefficient = data.getDouble("resistanceCoefficient", 0.2);
    private final double empoweredAbsorb = data.getDouble("empoweredAbsorb", 10);
    private final double playerCombatMultiplier = data.getDouble("playerCombatMultiplier", 0.65);

    private Location barrierLocation;
    private double accumulatedDamage = 0;
    private double remainingAbsorb;
    private boolean empowered;
    private AoE primaryBarrier;
    private TerrainRing paintedBoundary;

    public CelestialBarrier(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = duration * 20;
    }

    @Override
    protected boolean onPrecast() {
        Block block = caster.getTargetBlockExact(8);
        if (block == null) {
            barrierLocation = caster.getLocation();
        } else if (!block.getType().isSolid()) {
            barrierLocation = caster.getLocation();
        } else {
            barrierLocation = block.getLocation().add(0.5, 1, 0.5);
        }
        return super.onPrecast();
    }

    @Override
    public boolean onCast() {
        empowered = caster.getEnergy() >= minWrathForReflect;
        if (!super.onCast()) return false;
        if (empowered) caster.setEnergy(Math.max(0, caster.getEnergy() - minWrathForReflect));
        double resistance = Math.max(0, caster.getAttribute(org.bukkit.attribute.Attribute.RESISTANCE_PHYSICAL).getValue())
                + Math.max(0, caster.getAttribute(org.bukkit.attribute.Attribute.RESISTANCE_MAGICAL).getValue());
        remainingAbsorb = Math.min(caster.getMaxHealth() * 0.4,
                baseAbsorb + Math.min(100, resistance) * resistanceCoefficient
                        + (empowered ? empoweredAbsorb : 0));
        double healingPower = Math.min(100, Math.max(0,
                caster.getAttribute(org.bukkit.attribute.Attribute.STAT_HEALINGPOWER).getValue()));
        double healing = baseHealing + healingPower * healingPowerCoefficient + (empowered ? 8 : 0);
        healOne(caster, healing);
        if (caster.getTargetEntity(6, true) instanceof LivingEntity ally && ally != caster
                && !Spellbook.canAttack(caster, ally) && caster.hasLineOfSight(ally)) healOne(ally, healing);

        primaryBarrier = createCircularAoE(barrierLocation, radius, 3, keepAliveTicks)
                .onEnter((aoe, entity) -> {
                    if (!Spellbook.canAttack(caster, entity)) {
                        entity.getWorld().spawnParticle(Particle.ENCHANTED_HIT, entity.getLocation(), 5, 0.3, 0.5, 0.3);
                        entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_BEACON_AMBIENT, 0.5f, 1.5f);
                    }
                });

        if (SkillVfx.availableTo(caster)) {
            paintedBoundary = new TerrainRing(barrierLocation, radius,
                    "terrain_band_hierophant", 64, 0.65f);
        }

        barrierLocation.getWorld().playSound(barrierLocation, Sound.BLOCK_BEACON_ACTIVATE, 1.5f, 1.2f);

        Bukkit.getPluginManager().registerEvents(this, Spellbook.getInstance().getImplementer());
        return true;
    }

    private void healOne(LivingEntity target, double healing) {
        double capped = Math.min(target.getMaxHealth() * 0.25, healing);
        target.heal(HealingContext.isPlayerCombat(caster, target)
                ? capped * playerCombatMultiplier : capped);
        TargetedParticles.spawnForOthers(Particle.HEART, target,
                target.getEyeLocation(), 3, 0.3, 0.3, 0.3);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity damaged) || Spellbook.canAttack(caster, damaged)
                || damaged.getWorld() != barrierLocation.getWorld()
                || damaged.getLocation().distanceSquared(barrierLocation) > radius * radius
                || remainingAbsorb <= 0 || event.getDamage() <= 0) return;
        double prevented = Math.min(remainingAbsorb, event.getDamage());
        remainingAbsorb -= prevented;
        accumulatedDamage += prevented;
        event.setDamage(Math.max(0, event.getDamage() - prevented));
        barrierLocation.getWorld().playSound(damaged.getLocation(), Sound.BLOCK_GLASS_BREAK, 0.8f, 1.5f);
        barrierLocation.getWorld().spawnParticle(Particle.END_ROD, damaged.getLocation(), 8, 0.5, 1.0, 0.5);
        if (remainingAbsorb <= 0) currentTicks = keepAliveTicks;
    }

    @Override
    protected void onTickFinish() {
        double damageMultiplier = empowered ? bonusReflectDamageMultiplier : reflectDamageMultiplier;
        double magicalAdvantage = Math.max(0,
                caster.getAttribute(org.bukkit.attribute.Attribute.ADVANTAGE_MAGICAL).getValue());
        double damage = Math.min(caster.getMaxHealth() * 0.35,
                accumulatedDamage * Math.min(1.0, damageMultiplier) + Math.min(20, magicalAdvantage) * 0.2);
        for (LivingEntity target : barrierLocation.getWorld().getNearbyLivingEntities(
                barrierLocation, explosionRadius, 2.5, explosionRadius)) {
            if (!Spellbook.canAttack(caster, target) || !caster.hasLineOfSight(target)) continue;
            CombatSourceContext.runSpell(this, () -> target.damage(damage, caster, PDamageType.MAGIC));
            target.getWorld().spawnParticle(Particle.EXPLOSION, target.getLocation(), 1);
        }


        barrierLocation.getWorld().playSound(barrierLocation, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.8f);
        barrierLocation.getWorld().spawnParticle(Particle.EXPLOSION, barrierLocation, 5, 2.0, 1.0, 2.0);

        if (empowered) caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT,
                SoundCategory.RECORDS, 0.8f, 0.8f);

        super.onTickFinish();
    }

    @Override
    protected void cleanup() {
        if (paintedBoundary != null) paintedBoundary.close();
        if (primaryBarrier != null) primaryBarrier.end();
        HandlerList.unregisterAll(this);
        super.cleanup();
    }
}
