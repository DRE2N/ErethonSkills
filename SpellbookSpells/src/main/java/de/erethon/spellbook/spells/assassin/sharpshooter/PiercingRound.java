package de.erethon.spellbook.spells.assassin.sharpshooter;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.assassin.AssassinBaseSpell;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.LineEffect;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class PiercingRound extends AssassinBaseSpell {

    // Consume a portion of your charge to fire a fast, uncharged energy lance that pierces through all enemies in a line.
    // Deals moderate damage to each target hit. If this ability scores a headshot, 50% of its Focus cost is refunded.

    private final double lanceSpeed = data.getDouble("lanceSpeed", 3.0);
    private final double lanceRange = data.getDouble("lanceRange", 48.0);
    private final double lanceWidth = data.getDouble("lanceWidth", 0.8);
    private final double headshotRangeFromEye = data.getDouble("headshotRangeFromEye", 0.4);
    private final double headshotMultiplier = data.getDouble("headshotMultiplier", 1.5);
    private final double focusRefundPercentage = data.getDouble("focusRefundPercentage", 0.5);
    private final int focusPerHeadshot = data.getInt("focusPerHeadshot", 25);

    public PiercingRound(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onPrecast() {
        return super.onPrecast();
    }

    @Override
    public boolean onCast() {
        firePiercingLance();
        return super.onCast();
    }

    private void firePiercingLance() {
        Location startLoc = caster.getEyeLocation();
        Vector direction = startLoc.getDirection().normalize();

        caster.getWorld().playSound(startLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 0.6f, 2.0f);
        caster.getWorld().playSound(startLoc, Sound.ITEM_CROSSBOW_SHOOT, SoundCategory.PLAYERS, 1.0f, 1.8f);

        startLoc.getWorld().spawnParticle(Particle.FLASH, startLoc, 1, 0, 0, 0, 0);
        startLoc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, startLoc, 8, 0.2, 0.2, 0.2, 0.3);

        List<LivingEntity> hitTargets = new ArrayList<>();
        final boolean[] hadHeadshot = {false};

        new BukkitRunnable() {
            Location currentLoc = startLoc.clone();
            double distanceTraveled = 0;
            final int maxTicks = (int) (lanceRange / lanceSpeed);
            int ticks = 0;

            @Override
            public void run() {
                ticks++;

                Vector step = direction.clone().multiply(lanceSpeed);
                currentLoc.add(step);
                distanceTraveled += lanceSpeed;

                createLanceTrail(currentLoc);

                if (currentLoc.getBlock().getType().isSolid() || distanceTraveled >= lanceRange) {
                    createImpactEffect(currentLoc);
                    this.cancel();
                    return;
                }

                BoundingBox searchBox = BoundingBox.of(currentLoc, lanceWidth, lanceWidth, lanceWidth);
                for (LivingEntity entity : currentLoc.getNearbyLivingEntities(lanceWidth)) {
                    if (entity != caster && !hitTargets.contains(entity) && Spellbook.canAttack(caster, entity)) {
                        if (searchBox.contains(entity.getLocation().toVector())) {
                            hitTargets.add(entity);
                            boolean isHeadshot = checkHeadshot(entity, currentLoc);

                            if (isHeadshot && !hadHeadshot[0]) {
                                hadHeadshot[0] = true;
                            }

                            dealDamage(entity, isHeadshot);
                        }
                    }
                }
            }

            @Override
            public void cancel() {
                super.cancel();
                if (hadHeadshot[0]) {
                    int refundAmount = (int) (energyCost * focusRefundPercentage);
                    caster.addEnergy(refundAmount);
                    caster.addEnergy(focusPerHeadshot);

                    caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 0.8f, 1.8f);
                }

                createFullLanceEffect(startLoc, currentLoc);
            }
        }.runTaskTimer(Spellbook.getInstance().getImplementer(), 0L, 1L);
    }

    private void createLanceTrail(Location loc) {
        loc.getWorld().spawnParticle(Particle.DUST, loc, 2, 0.1, 0.1, 0.1, 0,
                new Particle.DustOptions(Color.PURPLE, 1.5f));
        loc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, loc, 1, 0.05, 0.05, 0.05, 0.1);
    }

    private void createImpactEffect(Location impactLoc) {
        impactLoc.getWorld().spawnParticle(Particle.EXPLOSION, impactLoc, 1, 0, 0, 0, 0);
        impactLoc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, impactLoc, 15, 0.5, 0.5, 0.5, 0.2);
        impactLoc.getWorld().playSound(impactLoc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.8f, 1.5f);
    }

    private void createFullLanceEffect(Location start, Location end) {
        EffectManager effectManager = Spellbook.getInstance().getEffectManager();
        if (effectManager != null) {
            LineEffect lanceEffect = new LineEffect(effectManager);
            lanceEffect.setLocation(start);
            lanceEffect.setTarget(end);
            lanceEffect.particle = Particle.DUST;
            lanceEffect.color = Color.WHITE;
            lanceEffect.particles = 50;
            lanceEffect.duration = 15;
            lanceEffect.start();
        }
    }

    private boolean checkHeadshot(LivingEntity target, Location hitLocation) {
        Location eyeLoc = target.getEyeLocation();
        BoundingBox headshotBox = new BoundingBox(
                eyeLoc.getX() - headshotRangeFromEye,
                eyeLoc.getY() - headshotRangeFromEye,
                eyeLoc.getZ() - headshotRangeFromEye,
                eyeLoc.getX() + headshotRangeFromEye,
                eyeLoc.getY() + headshotRangeFromEye,
                eyeLoc.getZ() + headshotRangeFromEye
        );

        return headshotBox.contains(hitLocation.toVector());
    }

    private void dealDamage(LivingEntity target, boolean isHeadshot) {
        double damage = Spellbook.getVariedAttributeBasedDamage(data, caster, target, true, Attribute.ADVANTAGE_MAGICAL);

        if (isHeadshot) {
            damage *= headshotMultiplier;
            target.getWorld().playSound(target.getLocation(), Sound.BLOCK_ANVIL_HIT, SoundCategory.PLAYERS, 1.0f, 1.2f);
            target.getWorld().spawnParticle(Particle.CRIT, target.getEyeLocation(), 12, 0.3, 0.3, 0.3, 0.3);
            target.getWorld().spawnParticle(Particle.DUST, target.getEyeLocation(), 8, 0.2, 0.2, 0.2, 0,
                    new Particle.DustOptions(Color.YELLOW, 1.8f));
        } else {
            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_ARROW_HIT, SoundCategory.PLAYERS, 0.8f, 1.0f);
            target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 6, 0.3, 0.3, 0.3, 0.2);
        }

        target.damage(damage, caster, PDamageType.MAGIC);

        target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(0, 1, 0), 5, 0.2, 0.2, 0.2, 0,
                new Particle.DustOptions(Color.PURPLE, 1.2f));
    }


    @Override
    protected void addSpellPlaceholders() {
        spellAddedPlaceholders.add(Component.text(headshotMultiplier, VALUE_COLOR));
        placeholderNames.add("headshotMultiplier");
        spellAddedPlaceholders.add(Component.text((int) (focusRefundPercentage * 100), VALUE_COLOR));
        placeholderNames.add("focusRefundPercentage");
    }
}
