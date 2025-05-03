package de.erethon.spellbook.spells.paladin.hierophant;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.paladin.PaladinBaseSpell;
import de.slikey.effectlib.effect.CylinderEffect;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public class CelestialBarrier extends PaladinBaseSpell implements Listener {

    // The Hierophant creates a celestial barrier of light that absorbs damage for a short duration.
    // When the barrier expires or breaks, it explodes, dealing damage to nearby enemies equal to 100 % of the damage absorbed.
    // If cast while you have more than 50 % wrath, the barrier reflects150% damage.

    private final double radius = data.getDouble("radius", 5.0);
    private final double explosionRadius = data.getDouble("explosionRadius", 7.0);
    private final int minWrathForReflect = data.getInt("minWrathForBonusReflect", 50);
    private final double reflectDamageMultiplier = data.getDouble("reflectDamageMultiplier", 1.0f);
    private final double bonusReflectDamageMultiplier = data.getDouble("bonusReflectDamageMultiplier", 1.5f);

    private Location barrierLocation;
    private double accumulatedDamage = 0;

    public CelestialBarrier(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = duration * 20;
    }

    @Override
    protected boolean onPrecast() {
        Block block = caster.getTargetBlockExact(32);
        if (block == null) {
            caster.sendParsedActionBar("<color:#ff0000>Invalid target!");
            return false;
        }
        if (!block.getType().isSolid()) {
            caster.sendParsedActionBar("<color:#ff0000>Invalid target!");
            return false;
        }
        barrierLocation = block.getLocation();
        return super.onPrecast();
    }

    @Override
    public boolean onCast() {
        CylinderEffect cylinderEffect = new CylinderEffect(Spellbook.getInstance().getEffectManager());
        cylinderEffect.setLocation(barrierLocation);
        cylinderEffect.radius = (float) radius;
        cylinderEffect.height = 2;
        cylinderEffect.particle = org.bukkit.Particle.END_ROD;
        cylinderEffect.iterations = 20;
        cylinderEffect.solid = false;
        cylinderEffect.particles = 50;
        cylinderEffect.enableRotation = true;
        cylinderEffect.duration = keepAliveTicks;
        cylinderEffect.start();
        Bukkit.getPluginManager().registerEvents(this, Spellbook.getInstance().getImplementer());
        return super.onCast();
    }

    @EventHandler
    private void onDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof LivingEntity damaged && event.getDamager() instanceof LivingEntity) {
            if (damaged.getLocation().distance(barrierLocation) <= radius && !Spellbook.canAttack(caster, damaged)) {
                accumulatedDamage += event.getDamage();
                barrierLocation.getWorld().playSound(damaged.getLocation(), org.bukkit.Sound.ENTITY_ENDER_DRAGON_FLAP, 1, 0.5f);
                barrierLocation.getWorld().spawnParticle(org.bukkit.Particle.END_ROD, damaged.getLocation(), 5, 0.5, 0.5, 0.5);
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    private void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof LivingEntity damaged && !Spellbook.canAttack(caster, damaged)) {
            if (damaged.getLocation().distance(barrierLocation) <= radius) {
                accumulatedDamage += event.getDamage();
                barrierLocation.getWorld().playSound(damaged.getLocation(), org.bukkit.Sound.ENTITY_ENDER_DRAGON_FLAP, 1, 0.5f);
                barrierLocation.getWorld().spawnParticle(org.bukkit.Particle.END_ROD, damaged.getLocation(), 5, 0.5, 0.5, 0.5);
                event.setCancelled(true);
            }
        }
    }

    @Override
    protected void onTickFinish() {
        for (LivingEntity livingEntity : barrierLocation.getNearbyLivingEntities(explosionRadius)) {
            if (Spellbook.canAttack(caster, livingEntity)) {
                double damage = accumulatedDamage * (caster.getEnergy() > minWrathForReflect ? reflectDamageMultiplier : bonusReflectDamageMultiplier);
                livingEntity.damage(damage, caster, PDamageType.MAGIC);
                barrierLocation.getWorld().spawnParticle(Particle.EXPLOSION, livingEntity.getLocation(), 1, 0.5, 0.5, 0.5);
                barrierLocation.getWorld().playSound(livingEntity.getLocation(), Sound.BLOCK_GLASS_BREAK, 1, 1.0f);
            }
        }
        super.onTickFinish();
    }

    @Override
    protected void cleanup() {
        HandlerList.unregisterAll(this);
        super.cleanup();
    }
}
