package de.erethon.spellbook.spells.assassin.sharpshooter;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.aoe.AoE;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.assassin.AssassinBaseSpell;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.SphereEffect;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Set;

public class GravitySnare extends SharpshooterBaseSpell {

    // Launch a small projectile that, upon impact with a surface or player, deploys a 4-block radius gravitational field for 3 seconds.
    // Enemies caught in the field are slowed, and slightly pulled towards the center.

    private final double projectileSpeed = data.getDouble("projectileSpeed", 1.5);
    private final double snareRadius = data.getDouble("snareRadius", 4.0);
    private final int snareDuration = data.getInt("snareDuration", 3) * 20;
    private final double pullStrength = data.getDouble("pullStrength", 0.3);
    private final double minSlowAmplifier = data.getDouble("minSlowAmplifier", 1.0);
    private final double maxSlowAmplifier = data.getDouble("maxSlowAmplifier", 3.0);
    private final double slowDuration = data.getDouble("slowDuration", 1.0) * 20;

    private final EffectData slowEffectData = Spellbook.getEffectData("Slowness");

    public GravitySnare(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onPrecast() {
        return super.onPrecast();
    }

    @Override
    public boolean onCast() {
        launchProjectile();
        return super.onCast();
    }

    private void launchProjectile() {
        Location startLoc = caster.getEyeLocation();
        Vector direction = startLoc.getDirection().normalize();

        caster.getWorld().playSound(startLoc, Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 0.8f, 1.8f);

        new BukkitRunnable() {
            final Location currentLoc = startLoc.clone();
            int ticks = 0;
            final int maxTicks = (int) (range / projectileSpeed);

            @Override
            public void run() {
                ticks++;

                currentLoc.add(direction.clone().multiply(projectileSpeed));

                currentLoc.getWorld().spawnParticle(Particle.DUST, currentLoc, 2, 0.1, 0.1, 0.1, 0,
                    new Particle.DustOptions(Color.PURPLE, 1.2f));
                currentLoc.getWorld().spawnParticle(Particle.WITCH, currentLoc, 1, 0, 0, 0, 0);

                Block hitBlock = currentLoc.getBlock();
                if (hitBlock.getType().isSolid() || ticks >= maxTicks) {
                    deploySnare(currentLoc);
                    this.cancel();
                    return;
                }

                for (LivingEntity entity : currentLoc.getNearbyLivingEntities(1.0)) {
                    if (entity != caster && Spellbook.canAttack(caster, entity)) {
                        deploySnare(currentLoc);
                        this.cancel();
                        return;
                    }
                }
            }
        }.runTaskTimer(Spellbook.getInstance().getImplementer(), 0L, 1L);
    }

    private void deploySnare(Location impactLocation) {
        Location snareCenter = impactLocation.clone();
        snareCenter.getWorld().playSound(snareCenter, Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 1.0f, 0.6f);
        snareCenter.getWorld().spawnParticle(Particle.PORTAL, snareCenter, 30, 2, 0.5, 2, 0.5);

        EffectManager effectManager = Spellbook.getInstance().getEffectManager();
        if (effectManager != null) {
            SphereEffect sphereEffect = new SphereEffect(effectManager);
            sphereEffect.setLocation(snareCenter);
            sphereEffect.radius = snareRadius;
            sphereEffect.particle = Particle.DUST;
            sphereEffect.color = Color.PURPLE;
            sphereEffect.particles = 20;
            sphereEffect.duration = snareDuration;
            sphereEffect.start();
        }

        createCircularAoE(snareCenter, snareRadius, 3.0, snareDuration)
            .onEnter((aoe, entity) -> {
                if (entity != caster && Spellbook.canAttack(caster, entity)) {
                    double slowIntensity = Spellbook.getRangedValue(data, caster, entity, Attribute.ADVANTAGE_MAGICAL, minSlowAmplifier, maxSlowAmplifier, "slowAmplifier");
                    entity.addEffect(caster, slowEffectData, (int) slowDuration, (int) slowIntensity);
                }
            })
            .onTick((aoe) -> {
                Set<LivingEntity> entitiesInField = aoe.getEntitiesInside();
                for (LivingEntity entity : entitiesInField) {
                    if (entity != caster && Spellbook.canAttack(caster, entity)) {
                        Vector pullDirection = snareCenter.toVector().subtract(entity.getLocation().toVector()).normalize();
                        Vector currentVelocity = entity.getVelocity();
                        Vector newVelocity = currentVelocity.add(pullDirection.multiply(pullStrength));
                        entity.setVelocity(newVelocity);

                        entity.getWorld().spawnParticle(Particle.DUST, entity.getLocation().add(0, 1, 0), 1, 0.2, 0.2, 0.2, 0,
                            new Particle.DustOptions(Color.PURPLE, 0.8f));
                    }
                }
            });
    }

    @Override
    protected void addSpellPlaceholders() {
        spellAddedPlaceholders.add(Component.text(Spellbook.getRangedValue(data, caster, caster, Attribute.ADVANTAGE_MAGICAL, minSlowAmplifier, maxSlowAmplifier, "slowAmplifier"), VALUE_COLOR));
        placeholderNames.add("slowAmplifier");

    }
}
