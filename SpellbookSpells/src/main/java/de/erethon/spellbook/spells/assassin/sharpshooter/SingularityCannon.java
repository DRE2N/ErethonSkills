package de.erethon.spellbook.spells.assassin.sharpshooter;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.assassin.AssassinBaseSpell;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.LineEffect;
import de.slikey.effectlib.effect.SphereEffect;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class SingularityCannon extends AssassinBaseSpell {

    // After a 1.5-second channel (caster is immobile), unleash a global-range, terrain-piercing beam of pure energy.
    // The beam deals immense damage to the first enemy player hit and 50% damage to any subsequent targets.
    // Enemies hit are marked with Singular Weakness for 5 seconds, causing them to take 20% increased damage from your Charged Shot.
    // If an enemy is killed by the initial beam, you instantly regain a full bar of Focus.

    private final double beamRange = data.getDouble("beamRange", 200.0);
    private final double beamWidth = data.getDouble("beamWidth", 1.5);
    private final double subsequentDamageMultiplier = data.getDouble("subsequentDamageMultiplier", 0.5);
    private final int singularWeaknessDuration = data.getInt("singularWeaknessDuration", 5) * 20;
    private final int maxFocusRefund = data.getInt("maxFocusRefund", 100);
    private final String singularWeaknessTag = "singular_weakness";

    private final int customChannelDuration = 30; // 1.5 seconds in ticks

    private Vector targetDirection;
    private boolean hasBeamFired = false;

    public SingularityCannon(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = customChannelDuration + 20; // Channel time + extra time for beam effects
    }

    @Override
    protected boolean onPrecast() {
        targetDirection = caster.getEyeLocation().getDirection().normalize();
        return super.onPrecast();
    }

    @Override
    public boolean onCast() {
        caster.setEnergy(0);
        createChannelStartVFX();
        caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 1.5f, 0.3f);
        caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_CONDUIT_AMBIENT, SoundCategory.PLAYERS, 2.0f, 0.5f);
        return super.onCast();
    }

    @Override
    protected void onTick() {
        super.onTick();

        if (currentTicks < customChannelDuration && !hasBeamFired) {
            createChannelVFX();
        }

        if (currentTicks == customChannelDuration && !hasBeamFired) {
            fireSingularityBeam();
            hasBeamFired = true;
        }
    }

    private void createChannelStartVFX() {
        Location eyeLoc = caster.getEyeLocation();

        EffectManager effectManager = Spellbook.getInstance().getEffectManager();
        if (effectManager != null) {
            SphereEffect chargeSphere = new SphereEffect(effectManager);
            chargeSphere.setLocation(eyeLoc);
            chargeSphere.radius = 3.0f;
            chargeSphere.particle = Particle.DUST;
            chargeSphere.color = Color.BLACK;
            chargeSphere.particles = 40;
            chargeSphere.duration = customChannelDuration;
            chargeSphere.start();
        }

        eyeLoc.getWorld().spawnParticle(Particle.PORTAL, eyeLoc, 30, 1.5, 1.5, 1.5, 0.5);
        eyeLoc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, eyeLoc, 20, 1, 1, 1, 0.3);
    }

    private void createChannelVFX() {
        Location eyeLoc = caster.getEyeLocation();
        double progress = (double) currentTicks / customChannelDuration;

        int particleCount = (int) (10 + (progress * 20));
        float radius = (float) (2.0 - progress);

        eyeLoc.getWorld().spawnParticle(Particle.DUST, eyeLoc, particleCount, radius, radius, radius, 0,
            new Particle.DustOptions(Color.fromRGB((int)(255 * progress), 0, (int)(255 * (1 - progress))), 1.5f));

        if (currentTicks % 5 == 0) {
            eyeLoc.getWorld().spawnParticle(Particle.FLASH, eyeLoc, 1, 0, 0, 0, 0);
            eyeLoc.getWorld().playSound(eyeLoc, Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 0.3f, (float) (1.5f + progress));
        }

        if (currentTicks > customChannelDuration / 2) {
            Location endLoc = eyeLoc.clone().add(targetDirection.clone().multiply(beamRange));
            EffectManager effectManager = Spellbook.getInstance().getEffectManager();
            if (effectManager != null) {
                LineEffect previewLine = new LineEffect(effectManager);
                previewLine.setLocation(eyeLoc);
                previewLine.setTarget(endLoc);
                previewLine.particle = Particle.DUST;
                previewLine.color = Color.PURPLE;
                previewLine.particles = 20;
                previewLine.duration = 5;
                previewLine.start();
            }
        }
    }

    private void fireSingularityBeam() {
        Location startLoc = caster.getEyeLocation();

        caster.getWorld().playSound(startLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 2.0f, 0.8f);
        caster.getWorld().playSound(startLoc, Sound.ENTITY_WITHER_SPAWN, SoundCategory.PLAYERS, 1.5f, 2.0f);
        caster.getWorld().playSound(startLoc, Sound.ENTITY_ENDER_DRAGON_SHOOT, SoundCategory.PLAYERS, 2.0f, 0.5f);

        startLoc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, startLoc, 1, 0, 0, 0, 0, null, true);
        startLoc.getWorld().spawnParticle(Particle.FLASH, startLoc, 3, 0.5, 0.5, 0.5, 0, null, true);
        startLoc.getWorld().spawnParticle(Particle.END_ROD, startLoc, 50, 2, 2, 2, 0.5, null, true);

        Location endLoc = startLoc.clone().add(targetDirection.clone().multiply(beamRange));
        createMassiveBeamEffect(startLoc, endLoc);

        List<LivingEntity> hitTargets = findTargetsInBeam(startLoc, targetDirection);

        for (int i = 0; i < hitTargets.size(); i++) {
            LivingEntity target = hitTargets.get(i);
            boolean isPrimaryTarget = (i == 0);

            double baseDamage = Spellbook.getVariedAttributeBasedDamage(data, caster, target, true, Attribute.ADVANTAGE_MAGICAL);
            if (!isPrimaryTarget) {
                baseDamage *= subsequentDamageMultiplier;
            }

            double targetHealthBefore = target.getHealth();
            target.damage(baseDamage, caster, PDamageType.MAGIC);

            target.getTags().add(singularWeaknessTag);
            new BukkitRunnable() {
                @Override
                public void run() {
                    target.getTags().remove(singularWeaknessTag);
                }
            }.runTaskLater(Spellbook.getInstance().getImplementer(), singularWeaknessDuration);

            createTargetHitVFX(target, isPrimaryTarget);

            if (isPrimaryTarget && target.getHealth() <= 0 && targetHealthBefore > 0) {
                caster.setEnergy(maxFocusRefund);
                createKillVFX(target.getLocation());
                caster.getWorld().playSound(caster.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 1.0f, 1.5f);
            }
        }

        createGlobalBeamEffects(startLoc, endLoc);
    }

    private void createMassiveBeamEffect(Location start, Location end) {
        EffectManager effectManager = Spellbook.getInstance().getEffectManager();
        if (effectManager != null) {
            LineEffect mainBeam = new LineEffect(effectManager);
            mainBeam.setLocation(start);
            mainBeam.setTarget(end);
            mainBeam.particle = Particle.DUST;
            mainBeam.color = Color.WHITE;
            mainBeam.particles = 100;
            mainBeam.duration = 40;
            mainBeam.particleForceShow = true;
            mainBeam.start();

            LineEffect energyBeam = new LineEffect(effectManager);
            energyBeam.setLocation(start);
            energyBeam.setTarget(end);
            energyBeam.particle = Particle.END_ROD;
            energyBeam.particles = 80;
            energyBeam.duration = 30;
            energyBeam.particleForceShow = true;
            energyBeam.start();
        }
    }

    private List<LivingEntity> findTargetsInBeam(Location start, Vector direction) {
        List<LivingEntity> targets = new ArrayList<>();

        for (double distance = 0; distance < beamRange; distance += 2.0) {
            Location checkLoc = start.clone().add(direction.clone().multiply(distance));

            for (LivingEntity entity : checkLoc.getNearbyLivingEntities(beamWidth)) {
                if (entity != caster && !targets.contains(entity) && Spellbook.canAttack(caster, entity)) {
                    if (entity instanceof Player) {
                        targets.add(0, entity); // Add players at the beginning for priority
                    } else {
                        targets.add(entity);
                    }
                }
            }
        }

        return targets;
    }

    private void createTargetHitVFX(LivingEntity target, boolean isPrimary) {
        Location targetLoc = target.getLocation().add(0, 1, 0);

        if (isPrimary) {
            targetLoc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, targetLoc, 1, 0, 0, 0, 0,null, true);
            targetLoc.getWorld().spawnParticle(Particle.FLASH, targetLoc, 3, 0.5, 0.5, 0.5, 0, null, true);
            targetLoc.getWorld().playSound(targetLoc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 2.0f, 0.8f);
        } else {
            targetLoc.getWorld().spawnParticle(Particle.EXPLOSION, targetLoc, 2, 0.3, 0.3, 0.3, 0,null, true);
            targetLoc.getWorld().playSound(targetLoc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.2f, 1.2f);
        }

        targetLoc.getWorld().spawnParticle(Particle.DUST, targetLoc, 20, 0.5, 0.5, 0.5, 0,
            new Particle.DustOptions(Color.RED, 2.0f), true);
        targetLoc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, targetLoc, 15, 0.8, 0.8, 0.8, 0.3,null, true);

        targetLoc.getWorld().spawnParticle(Particle.DUST, targetLoc, 10, 0.3, 0.3, 0.3, 0,
            new Particle.DustOptions(Color.PURPLE, 1.5f), true);
    }

    private void createKillVFX(Location killLoc) {
        killLoc.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, killLoc, 30, 1, 1, 1, 0.5,null, true);
        killLoc.getWorld().spawnParticle(Particle.FLASH, killLoc, 5, 1, 1, 1, 0,null, true);
        killLoc.getWorld().playSound(killLoc, Sound.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 1.0f, 1.8f);
    }

    private void createGlobalBeamEffects(Location start, Location end) {
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= 20) {
                    this.cancel();
                    return;
                }

                Vector direction = end.toVector().subtract(start.toVector()).normalize();
                double step = beamRange / 20.0;

                for (int i = 0; i < 10; i++) {
                    Location trailLoc = start.clone().add(direction.clone().multiply(i * step));
                    trailLoc.getWorld().spawnParticle(Particle.DUST, trailLoc, 3, 0.3, 0.3, 0.3, 0,
                        new Particle.DustOptions(Color.WHITE, 1.8f), true);

                    if (ticks < 5) {
                        trailLoc.getWorld().spawnParticle(Particle.END_ROD, trailLoc, 1, 0.1, 0.1, 0.1, 0.05,null, true);
                    }
                }

                ticks++;
            }
        }.runTaskTimer(Spellbook.getInstance().getImplementer(), 0L, 1L);

        for (Player player : start.getWorld().getPlayers()) {
            if (player.getLocation().distance(start) <= 64) {
                player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.PLAYERS, 0.5f, 0.3f);
            }
        }
    }

    @Override
    protected void addSpellPlaceholders() {
        spellAddedPlaceholders.add(Component.text(customChannelDuration / 20.0, VALUE_COLOR));
        placeholderNames.add("channelDuration");
        spellAddedPlaceholders.add(Component.text((int)(subsequentDamageMultiplier * 100), VALUE_COLOR));
        placeholderNames.add("subsequentDamageMultiplier");
        spellAddedPlaceholders.add(Component.text(singularWeaknessDuration / 20, VALUE_COLOR));
        placeholderNames.add("singularWeaknessDuration");
    }
}
