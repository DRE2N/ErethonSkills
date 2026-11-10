package de.erethon.spellbook.spells.assassin.sharpshooter;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.combat.CombatSourceContext;
import de.erethon.spellbook.combat.BossControl;
import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.spells.vfx.SkillSignatureVfx;
import de.erethon.spellbook.spells.vfx.SkillVfx;

import de.erethon.spellbook.spells.vfx.TerrainRing;
import net.kyori.adventure.text.Component;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.Set;
import java.util.HashSet;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

public class GravitySnare extends SharpshooterBaseSpell {

    // Launch a small projectile that, upon impact with a surface or player, deploys a 4-block radius gravitational field for 3 seconds.
    // Enemies caught in the field are slowed, and slightly pulled towards the center.

    private final double projectileSpeed = data.getDouble("projectileSpeed", 1.5);
    private final double snareRadius = data.getDouble("snareRadius", 4.0);
    private final int snareDuration = data.getInt("snareDuration", 3) * 20;
    private final double pullStrength = data.getDouble("pullStrength", 0.08);
    private final double minSlowAmplifier = data.getDouble("minSlowAmplifier", 1.0);
    private final double maxSlowAmplifier = data.getDouble("maxSlowAmplifier", 3.0);
    private final double slowDuration = data.getDouble("slowDuration", 1.0) * 20;

    private final EffectData slowEffectData = Spellbook.getEffectData("Slow");

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
        ItemDisplay projectile = SkillVfx.projectile(startLoc, direction,
                "sig_assassin_sharpshooter_gravitysnare_lead_f4", 1.4f);

        trackVfxTask(new BukkitRunnable() {
            final Location currentLoc = startLoc.clone();
            int ticks = 0;
            final int maxTicks = (int) (range / projectileSpeed);

            @Override
            public void run() {
                ticks++;

                RayTraceResult blockHit = currentLoc.getWorld().rayTraceBlocks(currentLoc, direction,
                        projectileSpeed, FluidCollisionMode.NEVER, true);
                if (blockHit != null && blockHit.getHitBlock() != null) {
                    Block hitBlock = blockHit.getHitBlock();
                    // Anchor the ground circle and center displays to the hit block.
                    deploySnare(hitBlock.getLocation().add(0.5, 1.0, 0.5));
                    projectile.remove();
                    this.cancel();
                    return;
                }

                currentLoc.add(direction.clone().multiply(projectileSpeed));
                projectile.teleport(currentLoc);
                if (ticks >= maxTicks) {
                    deploySnare(currentLoc);
                    projectile.remove();
                    this.cancel();
                    return;
                }

                for (LivingEntity entity : currentLoc.getNearbyLivingEntities(1.0)) {
                    if (entity != caster && Spellbook.canAttack(caster, entity)) {
                        deploySnare(currentLoc);
                        projectile.remove();
                        this.cancel();
                        return;
                    }
                }
            }
        }.runTaskTimer(Spellbook.getInstance().getImplementer(), 0L, 1L));
    }

    private void deploySnare(Location impactLocation) {
        Location snareCenter = impactLocation.clone();
        snareCenter.getWorld().playSound(snareCenter, Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 1.0f, 0.6f);
        createGravityLattice(snareCenter);
        // The visible ring arms before it begins controlling movement.
        org.bukkit.Bukkit.getScheduler().runTaskLater(Spellbook.getInstance().getImplementer(),
                () -> { if (caster.isValid()) activateSnare(snareCenter); }, 8L);
    }

    private void activateSnare(Location snareCenter) {
        final int[] fieldTicks = {0};
        Set<UUID> bossTargets = new HashSet<>();
        createCircularAoE(snareCenter, snareRadius, 3.0, snareDuration)
            .setParticlesEnabled(false)
            .onEnter((aoe, entity) -> {
                if (entity != caster && Spellbook.canAttack(caster, entity)) {
                    double damage = Spellbook.getVariedAttributeBasedDamage(data, caster, entity, true, Attribute.ADVANTAGE_MAGICAL);
                    double before = entity.getHealth() + entity.getAbsorptionAmount();
                    CombatSourceContext.runSpell(this, () -> entity.damage(damage, caster, PDamageType.MAGIC));
                    boolean confirmed = entity.getHealth() + entity.getAbsorptionAmount() < before;
                    if (BossControl.apply(caster, entity, confirmed ? 30 : 0)) {
                        bossTargets.add(entity.getUniqueId());
                        return;
                    }
                    double slowIntensity = Math.min(entity instanceof org.bukkit.entity.Player ? 1 : 2,
                            Spellbook.getRangedValue(data, caster, entity, Attribute.ADVANTAGE_MAGICAL,
                                    minSlowAmplifier, maxSlowAmplifier, "slowAmplifier"));
                    entity.addEffect(caster, slowEffectData, (int) slowDuration, (int) slowIntensity);
                }
            })
            .onTick((aoe) -> {
                if (++fieldTicks[0] % 5 != 0) return;
                Set<LivingEntity> entitiesInField = aoe.getEntitiesInside();
                for (LivingEntity entity : entitiesInField) {
                    if (entity != caster && Spellbook.canAttack(caster, entity) && !bossTargets.contains(entity.getUniqueId())) {
                        Vector pullDirection = snareCenter.toVector().subtract(entity.getLocation().toVector());
                        if (pullDirection.lengthSquared() < 0.01 || !entity.isOnGround()) continue;
                        pullDirection.setY(0).normalize();
                        Vector currentVelocity = entity.getVelocity();
                        Vector newVelocity = currentVelocity.add(pullDirection.multiply(pullStrength));
                        entity.setVelocity(newVelocity);

                    }
                }
            });
    }

    private void createGravityLattice(Location center) {
        TerrainRing outer = new TerrainRing(center, snareRadius,
                "terrain_band_sharpshooter", 48, 0.42f);
        TerrainRing inner = new TerrainRing(center, snareRadius * 0.55,
                "terrain_band_sharpshooter", 32, 0.3f);
        SkillSignatureVfx.accent(center, getClass(), "main", (float) (snareRadius * 1.8), 24, true);
        org.bukkit.Bukkit.getScheduler().runTaskLater(Spellbook.getInstance().getImplementer(),
                () -> { outer.close(); inner.close(); }, snareDuration + 8L);
    }
    @Override
    protected void addSpellPlaceholders() {
        spellAddedPlaceholders.add(Component.text(Spellbook.getRangedValue(data, caster, caster, Attribute.ADVANTAGE_MAGICAL, minSlowAmplifier, maxSlowAmplifier, "slowAmplifier"), VALUE_COLOR));
        placeholderNames.add("slowAmplifier");

    }
}
