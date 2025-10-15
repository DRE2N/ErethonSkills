package de.erethon.spellbook.spells.paladin.hierophant;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.aoe.AoE;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.paladin.PaladinBaseSpell;
import de.slikey.effectlib.effect.CylinderEffect;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

public class CelestialBarrier extends HierophantBaseSpell implements Listener {

    // The Hierophant creates a celestial barrier of light that absorbs damage for a short duration.
    // When the barrier expires or breaks, it explodes, dealing damage to nearby enemies equal to 100% of the damage absorbed.
    // If cast while you have more than 50% wrath, the barrier reflects 150% damage and creates sacred ground that heals allies.

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
            barrierLocation = caster.getLocation();
        } else if (!block.getType().isSolid()) {
            barrierLocation = caster.getLocation();
        } else {
            barrierLocation = block.getLocation().add(0, 1, 0);
        }
        return super.onPrecast();
    }

    @Override
    public boolean onCast() {
        boolean hasHighWrath = caster.getEnergy() > minWrathForReflect;

        AoE barrier = createCircularAoE(barrierLocation, radius, 3, keepAliveTicks)
                .onEnter((aoe, entity) -> {
                    if (!Spellbook.canAttack(caster, entity)) {
                        entity.getWorld().spawnParticle(Particle.ENCHANTED_HIT, entity.getLocation(), 5, 0.3, 0.5, 0.3);
                        entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_BEACON_AMBIENT, 0.5f, 1.5f);
                    }
                })
                .onTick(aoe -> {
                    for (LivingEntity entity : aoe.getEntitiesInside()) {
                        if (!Spellbook.canAttack(caster, entity)) {
                            entity.getWorld().spawnParticle(Particle.END_ROD, entity.getLocation(), 1, 0.2, 0.5, 0.2);
                            if (hasHighWrath && entity.getTicksLived() % 20 == 0) {
                                entity.heal(2.0);
                                entity.getWorld().spawnParticle(Particle.HEART, entity.getLocation().add(0, 2, 0), 1);
                            }
                        }
                    }
                });

        if (hasHighWrath) {
            barrier = createCircularAoE(barrierLocation, radius + 2, 1, keepAliveTicks)
                    .onTick(aoe -> {
                        for (LivingEntity entity : aoe.getEntitiesInside()) {
                            if (!Spellbook.canAttack(caster, entity)) {
                                entity.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, entity.getLocation(), 1, 0.3, 0.3, 0.3);
                            }
                        }
                    });
        }
        BlockDisplay blockDisplay = caster.getWorld().spawn(barrierLocation, BlockDisplay.class, bd -> {
            bd.setBlock(Material.BEACON.createBlockData());
            bd.setPersistent(false);
            bd.setTransformation(new Transformation(new Vector3f(0, 0, 0), new AxisAngle4f(0, 1, 0, 0), new Vector3f(2, 2, 2), new AxisAngle4f(0, 0, 0, 0)));
        });
        barrier.addDisplay(blockDisplay);

        CylinderEffect cylinderEffect = new CylinderEffect(Spellbook.getInstance().getEffectManager());
        cylinderEffect.setLocation(barrierLocation);
        cylinderEffect.radius = (float) radius;
        cylinderEffect.height = 3;
        cylinderEffect.particle = Particle.END_ROD;
        cylinderEffect.iterations = 20;
        cylinderEffect.solid = false;
        cylinderEffect.particles = hasHighWrath ? 80 : 50;
        cylinderEffect.enableRotation = true;
        cylinderEffect.duration = keepAliveTicks;
        cylinderEffect.start();

        barrierLocation.getWorld().playSound(barrierLocation, Sound.BLOCK_BEACON_ACTIVATE, 1.5f, 1.2f);

        Bukkit.getPluginManager().registerEvents(this, Spellbook.getInstance().getImplementer());
        return super.onCast();
    }

    @EventHandler
    private void onDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof LivingEntity damaged && event.getDamager() instanceof LivingEntity) {
            if (damaged.getLocation().distance(barrierLocation) <= radius && !Spellbook.canAttack(caster, damaged)) {
                accumulatedDamage += event.getDamage();
                barrierLocation.getWorld().playSound(damaged.getLocation(), Sound.BLOCK_GLASS_BREAK, 0.8f, 1.5f);
                barrierLocation.getWorld().spawnParticle(Particle.END_ROD, damaged.getLocation(), 8, 0.5, 1.0, 0.5);
                barrierLocation.getWorld().spawnParticle(Particle.ENCHANTED_HIT, damaged.getLocation(), 5, 0.3, 0.5, 0.3);
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    private void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof LivingEntity damaged && !Spellbook.canAttack(caster, damaged)) {
            if (damaged.getLocation().distance(barrierLocation) <= radius) {
                accumulatedDamage += event.getDamage();
                barrierLocation.getWorld().playSound(damaged.getLocation(), Sound.BLOCK_GLASS_BREAK, 0.8f, 1.5f);
                barrierLocation.getWorld().spawnParticle(Particle.END_ROD, damaged.getLocation(), 8, 0.5, 1.0, 0.5);
                barrierLocation.getWorld().spawnParticle(Particle.ENCHANTED_HIT, damaged.getLocation(), 5, 0.3, 0.5, 0.3);
                event.setCancelled(true);
            }
        }
    }

    @Override
    protected void onTickFinish() {
        boolean hasHighWrath = caster.getEnergy() > minWrathForReflect;
        double damageMultiplier = hasHighWrath ? bonusReflectDamageMultiplier : reflectDamageMultiplier;

        createCircularAoE(barrierLocation, explosionRadius, 2, 60)
                .onEnter((aoe, entity) -> {
                    if (Spellbook.canAttack(caster, entity)) {
                        double damage = accumulatedDamage * damageMultiplier;
                        entity.damage(damage, caster, PDamageType.MAGIC);
                        entity.getWorld().spawnParticle(Particle.EXPLOSION, entity.getLocation(), 1);
                    }
                })
                .addBlocksOnTopGroundLevel(Material.FIRE, Material.SOUL_FIRE)
                .sendBlockChanges();

        barrierLocation.getWorld().playSound(barrierLocation, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.8f);
        barrierLocation.getWorld().spawnParticle(Particle.EXPLOSION, barrierLocation, 5, 2.0, 1.0, 2.0);

        if (hasHighWrath) {
            caster.setEnergy(0);
            caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.RECORDS, 0.8f, 0.8f);
        }

        super.onTickFinish();
    }

    @Override
    protected void cleanup() {
        HandlerList.unregisterAll(this);
        super.cleanup();
    }
}
