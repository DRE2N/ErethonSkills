package de.erethon.spellbook.spells.assassin.sharpshooter;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.combat.CombatSourceContext;
import de.erethon.spellbook.spells.vfx.SkillSignatureVfx;
import de.erethon.spellbook.spells.vfx.SkillVfx;

import de.erethon.spellbook.spells.vfx.TerrainRing;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.bukkit.util.RayTraceResult;

import java.util.ArrayList;
import java.util.List;

public class SingularityCannon extends SharpshooterBaseSpell {

    // After a 1.5-second channel (caster is immobile), unleash a global-range, terrain-piercing beam of pure energy.
    // The beam deals immense damage to the first enemy player hit and 50% damage to any subsequent targets.
    // Enemies hit are marked with Singular Weakness for 5 seconds, causing them to take 20% increased damage from your Charged Shot.
    // If an enemy is killed by the initial beam, you instantly regain a full bar of Focus.

    private final double beamRange = data.getDouble("beamRange", 32.0);
    private final double beamWidth = data.getDouble("beamWidth", 1.5);
    private final double subsequentDamageMultiplier = data.getDouble("subsequentDamageMultiplier", 0.5);
    private final int singularWeaknessDuration = data.getInt("singularWeaknessDuration", 5) * 20;
    private final int maxFocusRefund = data.getInt("maxFocusRefund", 20);
    private final String singularWeaknessTag = "singular_weakness_" + caster.getUniqueId();

    private final int customChannelDuration = 30; // 1.5 seconds in ticks

    private Vector targetDirection;
    private boolean hasBeamFired = false;
    private boolean previewBuilt = false;
    private ItemDisplay cannonDisplay;

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
        Location muzzle = caster.getEyeLocation().add(targetDirection.clone().multiply(1.5));
        cannonDisplay = trackVfx(SkillVfx.billboard(muzzle,
                "sig_assassin_sharpshooter_singularitycannon_lead_f4", 2.5f));
        SkillSignatureVfx.accent(caster.getLocation(), getClass(), "lead", 3.4f, 23, true);
    }

    private void createChannelVFX() {
        Location muzzle = caster.getEyeLocation().add(targetDirection.clone().multiply(1.5));
        if (cannonDisplay != null && cannonDisplay.isValid()) cannonDisplay.teleport(muzzle);
        if (currentTicks % 5 == 0) {
            SkillSignatureVfx.accent(muzzle, getClass(), "wake",
                    1.6f + currentTicks / (float) customChannelDuration, 9, false);
            muzzle.getWorld().playSound(muzzle, Sound.BLOCK_BEACON_POWER_SELECT,
                    SoundCategory.PLAYERS, 0.3f,
                    1.5f + currentTicks / (float) customChannelDuration);
        }
        if (currentTicks > customChannelDuration / 2 && !previewBuilt) {
            buildPreview(caster.getEyeLocation());
            previewBuilt = true;
        }
    }

    private void buildPreview(Location start) {
        double visibleRange = visibleBeamRange(start);
        for (int i = 1; i <= 12; i++) SkillSignatureVfx.accent(
                start.clone().add(targetDirection.clone().multiply(visibleRange * i / 13.0)),
                getClass(), "lead", 0.9f, 18, false);
    }
    private void fireSingularityBeam() {
        Location startLoc = caster.getEyeLocation();
        double visibleRange = visibleBeamRange(startLoc);

        caster.getWorld().playSound(startLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 2.0f, 0.8f);
        caster.getWorld().playSound(startLoc, Sound.ENTITY_WITHER_SPAWN, SoundCategory.PLAYERS, 1.5f, 2.0f);
        caster.getWorld().playSound(startLoc, Sound.ENTITY_ENDER_DRAGON_SHOOT, SoundCategory.PLAYERS, 2.0f, 0.5f);

        SkillSignatureVfx.accent(startLoc.clone().add(targetDirection.clone().multiply(1.5)),
                getClass(), "main", 5.5f, 24, false);

        Location endLoc = startLoc.clone().add(targetDirection.clone().multiply(visibleRange));
        createMassiveBeamEffect(startLoc, endLoc);

        List<LivingEntity> hitTargets = findTargetsInBeam(startLoc, targetDirection, visibleRange);

        for (int i = 0; i < hitTargets.size(); i++) {
            LivingEntity target = hitTargets.get(i);
            boolean isPrimaryTarget = (i == 0);

            double baseDamage = Spellbook.getVariedAttributeBasedDamage(data, caster, target, true, Attribute.ADVANTAGE_MAGICAL);
            if (!isPrimaryTarget) {
                baseDamage *= subsequentDamageMultiplier;
            }

            double targetHealthBefore = target.getHealth() + target.getAbsorptionAmount();
            double damage = baseDamage;
            CombatSourceContext.runSpell(this, () -> target.damage(damage, caster, PDamageType.MAGIC));
            if (target.getHealth() + target.getAbsorptionAmount() >= targetHealthBefore) continue;

            target.getTags().add(singularWeaknessTag);
            new BukkitRunnable() {
                @Override
                public void run() {
                    target.getTags().remove(singularWeaknessTag);
                }
            }.runTaskLater(Spellbook.getInstance().getImplementer(), singularWeaknessDuration);

            createTargetHitVFX(target, isPrimaryTarget);

            if (isPrimaryTarget && target.getHealth() <= 0 && targetHealthBefore > 0) {
                caster.addEnergy(maxFocusRefund);
                createKillVFX(target.getLocation());
                caster.getWorld().playSound(caster.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 1.0f, 1.5f);
            }
        }

        createGlobalBeamEffects(startLoc);
    }

    private void createMassiveBeamEffect(Location start, Location end) {
        Vector span = end.toVector().subtract(start.toVector());
        int count = Math.max(4, Math.min(24, (int) Math.ceil(span.length() / 1.5)));
        for (int i = 0; i < count; i++) SkillSignatureVfx.accent(
                start.clone().add(span.clone().multiply((i + 0.5) / count)),
                getClass(), i % 3 == 0 ? "main" : "wake", i % 3 == 0 ? 2.2f : 1.4f,
                25, false);
    }
    private double visibleBeamRange(Location start) {
        RayTraceResult hit = start.getWorld().rayTraceBlocks(start, targetDirection, beamRange,
                FluidCollisionMode.NEVER, true);
        return hit == null ? beamRange : Math.max(0, hit.getHitPosition().distance(start.toVector()) - 0.1);
    }

    private List<LivingEntity> findTargetsInBeam(Location start, Vector direction, double visibleRange) {
        List<LivingEntity> targets = new ArrayList<>();

        for (double distance = 0; distance < visibleRange; distance += 1.0) {
            Location checkLoc = start.clone().add(direction.clone().multiply(distance));

            for (LivingEntity entity : checkLoc.getNearbyLivingEntities(beamWidth)) {
                if (entity != caster && !targets.contains(entity) && Spellbook.canAttack(caster, entity)) {
                    targets.add(entity);
                }
            }
        }

        return targets;
    }

    private void createTargetHitVFX(LivingEntity target, boolean isPrimary) {
        Location center = target.getLocation().add(0, 1, 0);
        SkillSignatureVfx.accent(center, getClass(), "main",
                isPrimary ? 5.0f : 3.1f, 24, false);
        if (isPrimary) {
            TerrainRing terrain = new TerrainRing(target.getLocation(), 2.1,
                    "terrain_band_sharpshooter", 32, 0.35f);
            org.bukkit.Bukkit.getScheduler().runTaskLater(Spellbook.getInstance().getImplementer(),
                    terrain::close, 23L);
        }
        center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE,
                SoundCategory.PLAYERS, isPrimary ? 2.0f : 1.2f, 0.8f);
    }

    private void createKillVFX(Location killLoc) {
        SkillSignatureVfx.accent(killLoc.clone().add(0, 1, 0), getClass(),
                "wake", 3.3f, 21, false);
        killLoc.getWorld().playSound(killLoc, Sound.ITEM_TOTEM_USE,
                SoundCategory.PLAYERS, 1.0f, 1.8f);
    }
    private void createGlobalBeamEffects(Location start) {
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
