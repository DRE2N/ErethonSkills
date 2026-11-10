package de.erethon.spellbook.spells.assassin.sharpshooter;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.combat.CombatSourceContext;
import de.erethon.spellbook.combat.BossControl;
import de.erethon.spellbook.spells.vfx.SkillSignatureVfx;
import de.erethon.spellbook.spells.vfx.SkillVfx;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.bukkit.util.RayTraceResult;

import java.util.ArrayList;
import java.util.List;

public class PiercingRound extends SharpshooterBaseSpell {

    // Consume a portion of your charge to fire a fast, uncharged energy lance that pierces through all enemies in a line.
    // Deals moderate damage to each target hit. If this ability scores a headshot, 50% of its Focus cost is refunded.

    private final double lanceSpeed = data.getDouble("lanceSpeed", 3.0);
    private final double lanceRange = data.getDouble("lanceRange", 32.0);
    private final double lanceWidth = data.getDouble("lanceWidth", 0.8);
    private final double headshotRangeFromEye = data.getDouble("headshotRangeFromEye", 0.4);
    private final double headshotMultiplier = data.getDouble("headshotMultiplier", 1.5);
    private final double focusRefundPercentage = data.getDouble("focusRefundPercentage", 0.5);
    private final int focusPerHeadshot = data.getInt("focusPerHeadshot", 25);

    public PiercingRound(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = (int) Math.ceil(lanceRange / Math.max(0.1, lanceSpeed)) + 2;
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

        startLoc.getWorld().spawnParticle(Particle.FLASH, startLoc, 1, 0, 0, 0, 0, Color.RED);
        createMuzzle(startLoc, direction);

        List<LivingEntity> hitTargets = new ArrayList<>();
        final boolean[] hadHeadshot = {false};
        ItemDisplay lance = trackVfx(SkillVfx.projectile(startLoc, direction,
                "sig_assassin_sharpshooter_piercinground_lead_f4", 1.7f));

        trackVfxTask(new BukkitRunnable() {
            Location currentLoc = startLoc.clone();
            double distanceTraveled = 0;

            @Override
            public void run() {
                Location previousLoc = currentLoc.clone();
                double remaining = lanceRange - distanceTraveled;
                if (remaining <= 0) { this.cancel(); return; }
                double stepLength = Math.min(lanceSpeed, remaining);
                RayTraceResult blockHit = currentLoc.getWorld().rayTraceBlocks(currentLoc, direction,
                        stepLength, FluidCollisionMode.NEVER, true);
                if (blockHit != null) stepLength = Math.max(0, blockHit.getHitPosition().distance(currentLoc.toVector()) - 0.05);
                Vector step = direction.clone().multiply(stepLength);
                currentLoc.add(step);
                distanceTraveled += stepLength;

                createLanceTrail(currentLoc);
                if (lance.isValid()) lance.teleport(currentLoc);

                BoundingBox searchBox = BoundingBox.of(previousLoc, currentLoc).expand(lanceWidth);
                for (LivingEntity entity : currentLoc.getNearbyLivingEntities(lanceSpeed + lanceWidth)) {
                    if (entity != caster && !hitTargets.contains(entity) && Spellbook.canAttack(caster, entity)) {
                        if (searchBox.overlaps(entity.getBoundingBox())) {
                            hitTargets.add(entity);
                            boolean isHeadshot = checkHeadshot(entity, previousLoc, currentLoc);
                            boolean confirmed = dealDamage(entity, isHeadshot);
                            if (confirmed && isHeadshot) hadHeadshot[0] = true;
                            if (hitTargets.size() >= 5) break;
                        }
                    }
                }
                if (blockHit != null || distanceTraveled >= lanceRange || hitTargets.size() >= 5) {
                    createImpactEffect(currentLoc);
                    this.cancel();
                }
            }

            @Override
            public void cancel() {
                super.cancel();
                if (lance.isValid()) lance.remove();
                if (hadHeadshot[0]) {
                    int refundAmount = Math.min(25, (int) (energyCost * focusRefundPercentage) + focusPerHeadshot);
                    caster.addEnergy(refundAmount);

                    caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 0.8f, 1.8f);
                }

                createFullLanceEffect(startLoc, currentLoc);
            }
        }.runTaskTimer(Spellbook.getInstance().getImplementer(), 0L, 1L));
    }

    private void createMuzzle(Location center, Vector direction) {
        SkillSignatureVfx.accent(center.clone().add(direction.clone().multiply(1.1)),
                getClass(), "lead", 2.1f, 10, false);
    }

    private void createLanceTrail(Location loc) {
        SkillSignatureVfx.accent(loc, getClass(), "wake", 0.9f, 10, false);
    }

    private void createImpactEffect(Location impactLoc) {
        SkillSignatureVfx.accent(impactLoc, getClass(), "main", 2.2f, 18, false);
        impactLoc.getWorld().playSound(impactLoc, Sound.ENTITY_GENERIC_EXPLODE,
                SoundCategory.PLAYERS, 0.8f, 1.5f);
    }

    private void createFullLanceEffect(Location start, Location end) {
        Vector span = end.toVector().subtract(start.toVector());
        int count = Math.max(2, Math.min(10, (int) Math.ceil(span.length() / 3.5)));
        for (int i = 0; i < count; i++) SkillSignatureVfx.accent(
                start.clone().add(span.clone().multiply((i + 0.5) / count)),
                getClass(), "wake", 1.4f, 17, false);
    }
    private boolean checkHeadshot(LivingEntity target, Location segmentStart, Location segmentEnd) {
        Location eyeLoc = target.getEyeLocation();
        BoundingBox headshotBox = new BoundingBox(
                eyeLoc.getX() - headshotRangeFromEye,
                eyeLoc.getY() - headshotRangeFromEye,
                eyeLoc.getZ() - headshotRangeFromEye,
                eyeLoc.getX() + headshotRangeFromEye,
                eyeLoc.getY() + headshotRangeFromEye,
                eyeLoc.getZ() + headshotRangeFromEye
        );

        Vector travel = segmentEnd.toVector().subtract(segmentStart.toVector());
        return headshotBox.rayTrace(segmentStart.toVector(), travel.normalize(), segmentStart.distance(segmentEnd)) != null;
    }

    private boolean dealDamage(LivingEntity target, boolean isHeadshot) {
        double damage = Spellbook.getVariedAttributeBasedDamage(data, caster, target, true, Attribute.ADVANTAGE_MAGICAL);
        double appliedDamage = isHeadshot ? damage * headshotMultiplier : damage;
        double before = target.getHealth() + target.getAbsorptionAmount();
        CombatSourceContext.runSpell(this, () -> target.damage(appliedDamage, caster, PDamageType.MAGIC));
        if (target.getHealth() + target.getAbsorptionAmount() >= before) return false;
        BossControl.apply(caster, target, isHeadshot ? 25 : 15);

        if (isHeadshot) {
            target.getWorld().playSound(target.getLocation(), Sound.BLOCK_ANVIL_HIT, SoundCategory.PLAYERS, 1.0f, 1.2f);
            target.getWorld().spawnParticle(Particle.CRIT, target.getEyeLocation(), 12, 0.3, 0.3, 0.3, 0.3);
            target.getWorld().spawnParticle(Particle.DUST, target.getEyeLocation(), 8, 0.2, 0.2, 0.2, 0,
                    new Particle.DustOptions(Color.YELLOW, 1.8f));
            createHeadshotRune(target);
        } else {
            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_ARROW_HIT, SoundCategory.PLAYERS, 0.8f, 1.0f);
            target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 6, 0.3, 0.3, 0.3, 0.2);
        }

        target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(0, 1, 0), 5, 0.2, 0.2, 0.2, 0,
                new Particle.DustOptions(Color.PURPLE, 1.2f));
        return true;
    }

    private void createHeadshotRune(LivingEntity target) {
        SkillSignatureVfx.accent(target.getEyeLocation().add(0, 0.35, 0), getClass(),
                "main", 3.2f, 21, false);
    }
    @Override
    protected void addSpellPlaceholders() {
        spellAddedPlaceholders.add(Component.text(headshotMultiplier, VALUE_COLOR));
        placeholderNames.add("headshotMultiplier");
        spellAddedPlaceholders.add(Component.text((int) (focusRefundPercentage * 100), VALUE_COLOR));
        placeholderNames.add("focusRefundPercentage");
    }
}
