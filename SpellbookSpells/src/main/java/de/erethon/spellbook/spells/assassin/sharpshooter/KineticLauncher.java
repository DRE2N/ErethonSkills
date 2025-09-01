package de.erethon.spellbook.spells.assassin.sharpshooter;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import de.erethon.spellbook.spells.assassin.AssassinBaseSpell;
import de.erethon.spellbook.traits.assassin.sharpshooter.KineticLauncherTrait;
import de.erethon.spellbook.utils.SpellbookCommonMessages;
import de.slikey.effectlib.effect.LineEffect;
import de.slikey.effectlib.effect.SphereEffect;
import de.slikey.effectlib.EffectManager;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class KineticLauncher extends AssassinBaseSpell {

    // RMB: Fire a kinetic anchor that attaches to terrain or structures.
    // Pressing RMB again while the anchor is active propels you rapidly towards the anchor's location, launching you into the air at the end of the pull.
    // The anchor expires after 5 seconds if not used.

    private final int range = data.getInt("range", 32);
    private final double pullSpeed = data.getDouble("pullSpeed", 2.0);
    double launchThreshold = data.getDouble("launchThreshold", 2.5);
    double forwardLaunchPower = data.getDouble("forwardLaunchPower", 1.0);
    double upwardLaunchPower = data.getDouble("upwardLaunchPower", 1.2);

    private final TraitData kineticLauncherTraitData = Bukkit.getServer().getSpellbookAPI().getLibrary().getTraitByID("KineticLauncherTrait");

    private KineticLauncherTrait launcherTrait = null;
    private boolean hasLaunched = false;

    public KineticLauncher(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = duration * 20;
    }

    @Override
    protected boolean onPrecast() {
        getOrAddLauncherTrait();
        if (launcherTrait.getTargetLocation() != null && !launcherTrait.hasLaunched()) {
            launcherTrait.setHasLaunched(true);
            createPullSound();
            return false;
        }
        return super.onPrecast();
    }

    @Override
    public boolean onCast() {
        Block targetBlock = caster.getTargetBlockExact(range);
        if (targetBlock == null) {
            caster.sendParsedActionBar(SpellbookCommonMessages.NO_TARGET_BLOCK);
            return false;
        }
        if (!targetBlock.getType().isSolid()) {
            caster.sendParsedActionBar(SpellbookCommonMessages.NO_TARGET_BLOCK);
            return false;
        }
        launcherTrait.setTargetLocation(targetBlock.getLocation());

        createAnchorVFX(targetBlock.getLocation());
        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_BREEZE_WIND_BURST, SoundCategory.RECORDS, 1, 0.5f);
        caster.getWorld().playSound(targetBlock.getLocation(), Sound.BLOCK_ANVIL_LAND, SoundCategory.RECORDS, 0.8f, 1.2f);

        return super.onCast();
    }

    @Override
    protected void onTick() {
        super.onTick();
        if (launcherTrait.hasArrived()) {
            currentTicks = keepAliveTicks;
            onTickFinish();
            return;
        }
        if (launcherTrait.hasLaunched()) {
            Location anchorLocation = launcherTrait.getTargetLocation();
            Location playerLocation = caster.getLocation();

            double distanceToAnchor = playerLocation.distance(anchorLocation);
            if (distanceToAnchor <= launchThreshold) {
                launcherTrait.setHasArrived(true);
                Vector playerLookDirection = caster.getLocation().getDirection();
                Vector baseLaunchDirection = anchorLocation.toVector().subtract(playerLocation.toVector()).normalize();
                Vector baseLaunchVelocity = baseLaunchDirection.multiply(forwardLaunchPower)
                        .add(new Vector(0, upwardLaunchPower, 0));
                Vector influencedVelocity = baseLaunchVelocity.add(playerLookDirection.multiply(0.4));
                caster.setVelocity(influencedVelocity);

                createLaunchVFX(playerLocation, anchorLocation);
                caster.getWorld().playSound(playerLocation, Sound.ENTITY_BREEZE_JUMP, SoundCategory.RECORDS, 1.2f, 0.8f);

            } else {
                Vector pullDirection = anchorLocation.toVector().subtract(playerLocation.toVector()).normalize();
                caster.setVelocity(pullDirection.multiply(pullSpeed));

                createPullVFX(playerLocation, anchorLocation);
            }
        }
        createTetherLine();
    }

    @Override
    protected void onTickFinish() {
        super.onTickFinish();
        caster.removeTrait(kineticLauncherTraitData);
    }

    private void getOrAddLauncherTrait() {
        for (SpellTrait trait : caster.getActiveTraits()) {
            if (trait instanceof KineticLauncherTrait kineticLauncherTrait) {
                launcherTrait = kineticLauncherTrait;
                return;
            }
        }
        if (launcherTrait == null) {;
            caster.addTrait(kineticLauncherTraitData);
            for (SpellTrait trait : caster.getActiveTraits()) {
                if (trait instanceof KineticLauncherTrait kineticLauncherTrait) {
                    launcherTrait = kineticLauncherTrait;
                    break;
                }
            }
        }
    }

    private void createAnchorVFX(Location anchorLocation) {
        EffectManager effectManager = Spellbook.getInstance().getEffectManager();
        if (effectManager != null) {
            SphereEffect anchorSphere = new SphereEffect(effectManager);
            anchorSphere.setLocation(anchorLocation.clone().add(0, 0.5, 0));
            anchorSphere.radius = 1.0f;
            anchorSphere.particle = Particle.DUST;
            anchorSphere.color = Color.TEAL;
            anchorSphere.particles = 25;
            anchorSphere.duration = 20;
            anchorSphere.start();
        }

        anchorLocation.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, anchorLocation.clone().add(0, 0.5, 0), 15, 0.5, 0.5, 0.5, 0.1);
        anchorLocation.getWorld().spawnParticle(Particle.CRIT, anchorLocation.clone().add(0, 0.5, 0), 8, 0.3, 0.3, 0.3, 0.2);
    }

    private void createTetherLine() {
        if (launcherTrait.getTargetLocation() == null) return;

        EffectManager effectManager = Spellbook.getInstance().getEffectManager();
        if (effectManager != null) {
            LineEffect lineEffect = new LineEffect(effectManager);
            lineEffect.setLocation(caster.getLocation().add(0, -0.5, 0));
            lineEffect.setTarget(launcherTrait.getTargetLocation());
            lineEffect.particle = Particle.DUST;
            lineEffect.color = launcherTrait.hasLaunched() ? Color.LIME : Color.TEAL;
            lineEffect.duration = 20;
            lineEffect.particles = launcherTrait.hasLaunched() ? 30 : 20;
            lineEffect.start();
        }
    }

    private void createPullVFX(Location playerLocation, Location anchorLocation) {
        playerLocation.getWorld().spawnParticle(Particle.DUST, playerLocation.clone().add(0, 1, 0), 3, 0.2, 0.2, 0.2, 0,
            new Particle.DustOptions(Color.LIME, 1.0f));

        Vector direction = anchorLocation.toVector().subtract(playerLocation.toVector()).normalize();
        for (int i = 1; i <= 3; i++) {
            Location trailLoc = playerLocation.clone().add(direction.clone().multiply(-i * 0.5));
            trailLoc.getWorld().spawnParticle(Particle.DUST, trailLoc.add(0, 1, 0), 1, 0.1, 0.1, 0.1, 0,
                new Particle.DustOptions(Color.AQUA, 0.8f));
        }
    }

    private void createLaunchVFX(Location playerLocation, Location anchorLocation) {
        playerLocation.getWorld().spawnParticle(Particle.EXPLOSION, playerLocation, 1, 0, 0, 0, 0);
        playerLocation.getWorld().spawnParticle(Particle.CLOUD, playerLocation, 10, 0.5, 0.5, 0.5, 0.2);
        playerLocation.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, playerLocation, 20, 1, 1, 1, 0.3);

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= 10) {
                    this.cancel();
                    return;
                }

                Location currentLoc = caster.getLocation();
                currentLoc.getWorld().spawnParticle(Particle.DUST, currentLoc.add(0, 1, 0), 2, 0.3, 0.3, 0.3, 0,
                    new Particle.DustOptions(Color.WHITE, 1.2f));

                ticks++;
            }
        }.runTaskTimer(Spellbook.getInstance().getImplementer(), 0L, 2L);
    }

    private void createPullSound() {
        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_BREEZE_WIND_BURST, SoundCategory.RECORDS, 1.2f, 1.5f);
    }
}
