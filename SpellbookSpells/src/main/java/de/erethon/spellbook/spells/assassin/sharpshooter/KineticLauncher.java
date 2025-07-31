package de.erethon.spellbook.spells.assassin.sharpshooter;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import de.erethon.spellbook.spells.assassin.AssassinBaseSpell;
import de.erethon.spellbook.traits.assassin.sharpshooter.KineticLauncherTrait;
import de.slikey.effectlib.effect.LineEffect;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import java.awt.*;

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
            return false;
        }
        return super.onPrecast();
    }

    @Override
    public boolean onCast() {
        Block targetBlock = caster.getTargetBlockExact(range);
        if (targetBlock == null) {
            caster.sendParsedActionBar("<red>No valid target block in range.");
            return false;
        }
        if (!targetBlock.getType().isSolid()) {
            caster.sendParsedActionBar("<red>Cannot anchor to non-solid blocks.");
            return false;
        }
        launcherTrait.setTargetLocation(targetBlock.getLocation());
        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_BREEZE_WIND_BURST, SoundCategory.RECORDS,1, 0.5f);
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

            } else {
                Vector pullDirection = anchorLocation.toVector().subtract(playerLocation.toVector()).normalize();
                caster.setVelocity(pullDirection.multiply(pullSpeed));
            }
        }
        LineEffect lineEffect = new LineEffect(Spellbook.getInstance().getEffectManager());
        lineEffect.setLocation(caster.getLocation().add(0, -0.5, 0));
        lineEffect.setTarget(launcherTrait.getTargetLocation());
        lineEffect.particle = Particle.DUST;
        lineEffect.particleSize = 0.3f;
        lineEffect.color = Color.TEAL;
        lineEffect.duration = 20;
        lineEffect.start();
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
}
