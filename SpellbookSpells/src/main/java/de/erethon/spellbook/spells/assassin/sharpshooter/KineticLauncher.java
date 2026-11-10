package de.erethon.spellbook.spells.assassin.sharpshooter;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.utils.SpellbookFeedback;

import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import de.erethon.spellbook.spells.vfx.SkillSignatureVfx;
import de.erethon.spellbook.spells.vfx.SkillVfx;

import de.erethon.spellbook.traits.assassin.sharpshooter.KineticLauncherTrait;
import de.erethon.spellbook.utils.SpellbookCommonMessages;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.FluidCollisionMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class KineticLauncher extends SharpshooterBaseSpell {

    // RMB: Fire a kinetic anchor that attaches to terrain or structures.
    // Pressing RMB again while the anchor is active propels you rapidly towards the anchor's location, launching you into the air at the end of the pull.
    // The anchor expires after 5 seconds if not used.

    private static final NamespacedKey GUARD_KEY = new NamespacedKey("spellbook", "kinetic_launcher_guard");
    private final int range = data.getInt("range", 24);
    private final double pullSpeed = data.getDouble("pullSpeed", 1.0);
    double launchThreshold = data.getDouble("launchThreshold", 2.5);
    double forwardLaunchPower = data.getDouble("forwardLaunchPower", 0.8);
    double upwardLaunchPower = data.getDouble("upwardLaunchPower", 0.75);

    private final TraitData kineticLauncherTraitData = Bukkit.getServer().getSpellbookAPI().getLibrary().getTraitByID("KineticLauncherTrait");

    private KineticLauncherTrait launcherTrait = null;
    private Block selectedBlock;
    private ItemDisplay anchorDisplay;
    private final List<ItemDisplay> tetherSegments = new ArrayList<>();

    public KineticLauncher(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = duration * 20;
    }

    @Override
    protected boolean onPrecast() {
        findLauncherTrait();
        if (launcherTrait != null && launcherTrait.getTargetLocation() != null && !launcherTrait.hasLaunched()) {
            if (!hasAnchorSight(launcherTrait.getTargetLocation())) {
                SpellbookFeedback.send(caster, "<red>The anchor is blocked or out of range.");
                return false;
            }
            launcherTrait.setHasLaunched(true);
            createPullSound();
            return false;
        }
        if (!super.onPrecast()) return false;
        selectedBlock = caster.getTargetBlockExact(range);
        if (selectedBlock == null || !selectedBlock.getType().isSolid()) {
            SpellbookFeedback.send(caster, SpellbookCommonMessages.NO_TARGET_BLOCK);
            return false;
        }
        if (launcherTrait == null) getOrAddLauncherTrait();
        return launcherTrait != null;
    }

    @Override
    public boolean onCast() {
        if (!super.onCast()) return false;
        launcherTrait.setTargetLocation(selectedBlock.getLocation());
        createAnchorVFX(selectedBlock.getLocation());
        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_BREEZE_WIND_BURST, SoundCategory.RECORDS, 1, 0.5f);
        caster.getWorld().playSound(selectedBlock.getLocation(), Sound.BLOCK_ANVIL_LAND, SoundCategory.RECORDS, 0.8f, 1.2f);

        return true;
    }

    private boolean hasAnchorSight(Location anchor) {
        if (anchor == null || anchor.getWorld() != caster.getWorld()) return false;
        Location eye = caster.getEyeLocation();
        Vector toAnchor = anchor.clone().add(0.5, 0.5, 0.5).toVector().subtract(eye.toVector());
        double distance = toAnchor.length();
        return distance <= range && (distance < 1.0 || eye.getWorld().rayTraceBlocks(eye,
                toAnchor.normalize(), distance - 0.75, FluidCollisionMode.NEVER, true) == null);
    }

    @Override
    protected void onTick() {
        super.onTick();
        if (launcherTrait.hasArrived()) {
            currentTicks = keepAliveTicks;
            return;
        }
        if (launcherTrait.hasLaunched()) {
            Location anchorLocation = launcherTrait.getTargetLocation();
            Location playerLocation = caster.getLocation();
            if (!hasAnchorSight(anchorLocation)) {
                launcherTrait.setHasArrived(true);
                return;
            }

            double distanceToAnchor = playerLocation.distance(anchorLocation);
            if (distanceToAnchor <= launchThreshold) {
                launcherTrait.setHasArrived(true);
                Vector playerLookDirection = caster.getLocation().getDirection();
                Vector baseLaunchDirection = anchorLocation.toVector().subtract(playerLocation.toVector()).normalize();
                Vector baseLaunchVelocity = baseLaunchDirection.multiply(forwardLaunchPower)
                        .add(new Vector(0, upwardLaunchPower, 0));
                Vector influencedVelocity = baseLaunchVelocity.add(playerLookDirection.multiply(0.2));
                caster.setVelocity(influencedVelocity);
                grantLandingGuard();

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
        findLauncherTrait();
        if (launcherTrait != null) return;
        caster.addTrait(kineticLauncherTraitData);
        findLauncherTrait();
    }

    private void findLauncherTrait() {
        for (SpellTrait trait : caster.getActiveTraits()) {
            if (trait instanceof KineticLauncherTrait kineticLauncherTrait) {
                launcherTrait = kineticLauncherTrait;
                return;
            }
        }
        launcherTrait = null;
    }

    private void grantLandingGuard() {
        for (Attribute attribute : new Attribute[]{Attribute.RESISTANCE_PHYSICAL, Attribute.RESISTANCE_MAGICAL}) {
            var instance = caster.getAttribute(attribute);
            if (instance == null) continue;
            if (instance.getModifier(GUARD_KEY) != null) instance.removeModifier(GUARD_KEY);
            double bonus = Math.min(8, 1.5 + Math.max(0, instance.getValue()) * 0.06);
            instance.addTransientModifier(new AttributeModifier(GUARD_KEY, bonus,
                    AttributeModifier.Operation.ADD_NUMBER));
        }
        Bukkit.getScheduler().runTaskLater(Spellbook.getInstance().getImplementer(), () -> {
            for (Attribute attribute : new Attribute[]{Attribute.RESISTANCE_PHYSICAL, Attribute.RESISTANCE_MAGICAL}) {
                var instance = caster.getAttribute(attribute);
                if (instance != null && instance.getModifier(GUARD_KEY) != null) instance.removeModifier(GUARD_KEY);
            }
        }, 20L);
    }

    private void createAnchorVFX(Location anchorLocation) {
        Location center = anchorLocation.clone().add(0.5, 0.62, 0.5);
        anchorDisplay = trackVfx(SkillVfx.billboard(center,
                "sig_assassin_sharpshooter_kineticlauncher_f4", 2.5f));
        SkillSignatureVfx.accent(center, getClass(), "lead", 2.3f, 17, false);
    }

    private void createTetherLine() {
        if (launcherTrait.getTargetLocation() == null) return;
        Location start = caster.getLocation().add(0, 0.65, 0);
        Location end = launcherTrait.getTargetLocation().clone().add(0.5, 0.62, 0.5);
        Vector span = end.toVector().subtract(start.toVector());
        if (tetherSegments.isEmpty()) {
            for (int i = 0; i < 6; i++) tetherSegments.add(trackVfx(SkillVfx.projectile(start, span,
                    "sig_assassin_sharpshooter_kineticlauncher_lead_f4", 0.65f)));
        }
        for (int i = 0; i < tetherSegments.size(); i++) {
            ItemDisplay segment = tetherSegments.get(i);
            if (segment.isValid()) {
                segment.teleport(start.clone().add(span.clone().multiply(
                        (i + 0.5) / tetherSegments.size())));
                SkillVfx.orientProjectile(segment, span);
            }
        }
    }

    private void createPullVFX(Location playerLocation, Location anchorLocation) {
        if (currentTicks % 3 == 0) SkillSignatureVfx.accent(playerLocation.clone().add(0, 0.6, 0),
                getClass(), "wake", 0.9f, 9, false);
    }

    private void createLaunchVFX(Location playerLocation, Location anchorLocation) {
        SkillSignatureVfx.accent(playerLocation.clone().add(0, 1, 0), getClass(),
                "main", 3.1f, 19, false);
    }
    private void createPullSound() {
        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_BREEZE_WIND_BURST, SoundCategory.RECORDS, 1.2f, 1.5f);
    }
}
