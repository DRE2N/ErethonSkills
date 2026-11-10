package de.erethon.spellbook.spells.warrior.bladeweaver;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.combat.CombatSourceContext;
import de.erethon.spellbook.spells.vfx.SkillVfx;
import de.erethon.spellbook.spells.vfx.TerrainRing;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;


/**
 * Blade Step - Short range forward dash with brief invincibility.
 * In demon form: Leaves a damaging afterimage with orbiting spectral swords that explodes after a short delay.
 */
public class BladeStep extends BladeweaverBaseSpell {

    private static final NamespacedKey READY_KEY = new NamespacedKey("spellbook", "bladeweaver_step_ready_at");

    private final double dashSpeed = data.getDouble("dashSpeed", 1.1);
    private final int dashDurationTicks = data.getInt("dashDurationTicks", 2);
    private final int invincibilityTicks = data.getInt("invincibilityTicks", 3);
    private final int cooldownSeconds = data.getInt("cooldown", 12);
    private final double afterimageDamageRadius = data.getDouble("afterimageDamageRadius", 3.0);
    private final int afterimageDelay = data.getInt("afterimageDelay", 15);

    private final AttributeModifier invincibilityModifier = new AttributeModifier(
        NamespacedKey.fromString("spellbook:bladeweaver_bladestep_invincible"),
        9999.0,
        AttributeModifier.Operation.ADD_NUMBER
    );

    private Location startLocation;
    private Location afterimageLocation;
    private int afterimageTimer = -1;
    private boolean invincible = false;
    private boolean dashing = false;
    private Vector dashDirection;
    private int dashTicksRemaining = 0;

    private TerrainRing afterimageRing;

    public BladeStep(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = invincibilityTicks + afterimageDelay + 10;
    }

    @Override
    protected boolean onPrecast() {
        long readyAt = caster.getPersistentDataContainer().getOrDefault(READY_KEY, PersistentDataType.LONG, 0L);
        return System.currentTimeMillis() >= readyAt && super.onPrecast();
    }

    @Override
    public boolean onCast() {
        caster.getPersistentDataContainer().set(READY_KEY, PersistentDataType.LONG,
                System.currentTimeMillis() + cooldownSeconds * 1000L);
        startLocation = caster.getLocation().clone();
        dashDirection = caster.getLocation().getDirection().setY(0).normalize();

        spawnDepartureEffect(startLocation);

        if (!caster.getAttribute(Attribute.RESISTANCE_PHYSICAL).getModifiers().contains(invincibilityModifier)) {
            caster.getAttribute(Attribute.RESISTANCE_PHYSICAL).addTransientModifier(invincibilityModifier);
        }
        if (!caster.getAttribute(Attribute.RESISTANCE_MAGICAL).getModifiers().contains(invincibilityModifier)) {
            caster.getAttribute(Attribute.RESISTANCE_MAGICAL).addTransientModifier(invincibilityModifier);
        }
        invincible = true;

        dashing = true;
        dashTicksRemaining = dashDurationTicks;

        caster.getWorld().playSound(startLocation, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.5f);

        if (isInDemonForm()) {
            afterimageLocation = startLocation.clone();
            afterimageTimer = afterimageDelay;
            createAfterimageAnimation();
        }

        return super.onCast();
    }

    @Override
    protected void onTick() {
        super.onTick();

        // Handle dash velocity
        if (dashing && dashTicksRemaining > 0) {
            caster.setVelocity(dashDirection.clone().multiply(dashSpeed));
            dashTicksRemaining--;

            // Spawn trail particles
            Particle.DustOptions dust = new Particle.DustOptions(getThemeColor(), 1.0f);
            caster.getWorld().spawnParticle(Particle.DUST, caster.getLocation().add(0, 1, 0), 5, 0.2, 0.3, 0.2, 0, dust);

            if (dashTicksRemaining == 0) {
                dashing = false;
                spawnArrivalEffect(caster.getLocation());
                caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.6f, 1.8f);
            }
        }

        if (invincible && currentTicks >= invincibilityTicks) {
            removeInvincibility();
        }

        if (afterimageTimer > 0) {
            afterimageTimer--;
            updateAfterimageAnimation();
            if (afterimageTimer == 0) {
                triggerAfterimageExplosion();
            }
        }
    }

    private void spawnDepartureEffect(Location loc) {
        Location effectLoc = loc.clone().add(0, 1, 0);
        bladeFlash(effectLoc, "", 2.5f, 14);
        Particle.DustOptions dust = new Particle.DustOptions(getThemeColor(), 1.2f);
        effectLoc.getWorld().spawnParticle(Particle.DUST, effectLoc, 30, 0.3, 0.5, 0.3, 0, dust);
        effectLoc.getWorld().spawnParticle(Particle.SWEEP_ATTACK, effectLoc, 5, 0.5, 0.5, 0.5, 0);
    }

    private void spawnArrivalEffect(Location loc) {
        Location effectLoc = loc.clone().add(0, 1, 0);
        bladeFlash(effectLoc, "", 1.8f, 12);
        Particle.DustOptions dust = new Particle.DustOptions(getThemeColor(), 1.2f);
        effectLoc.getWorld().spawnParticle(Particle.DUST, effectLoc, 20, 0.3, 0.5, 0.3, 0, dust);
        effectLoc.getWorld().spawnParticle(Particle.END_ROD, effectLoc, 10, 0.2, 0.3, 0.2, 0.05);
    }

    private void createAfterimageAnimation() {
        Location center = afterimageLocation.clone().add(0, 1, 0);
        if (SkillVfx.availableTo(caster)) {
            afterimageRing = new TerrainRing(afterimageLocation, afterimageDamageRadius,
                    "terrain_band_bladeweaver");
            bladeGroundFlash(afterimageLocation.clone().add(0, 0.06, 0),
                    "", 2.7f, afterimageDelay);
        }

        Particle.DustOptions demonDust = new Particle.DustOptions(BLADEWEAVER_DEMON, 1.5f);
        for (double y = 0; y < 2; y += 0.2) {
            center.getWorld().spawnParticle(Particle.DUST, afterimageLocation.clone().add(0, y, 0),
                8, 0.15, 0.05, 0.15, 0, demonDust);
        }
    }

    private void updateAfterimageAnimation() {
        if (afterimageLocation == null) return;

        Location center = afterimageLocation.clone().add(0, 1, 0);

        if (afterimageTimer % 3 == 0) {
            Particle.DustOptions demonDust = new Particle.DustOptions(BLADEWEAVER_DEMON, 1.0f);
            center.getWorld().spawnParticle(Particle.DUST, center, 8, 0.3, 0.5, 0.3, 0, demonDust);
            center.getWorld().spawnParticle(Particle.FLAME, center, 3, 0.2, 0.3, 0.2, 0.02);
        }
    }

    private void triggerAfterimageExplosion() {
        if (afterimageLocation == null) return;

        if (afterimageRing != null) {
            afterimageRing.close();
            afterimageRing = null;
        }

        Location center = afterimageLocation.clone().add(0, 1, 0);
        bladeFlash(center, "", 3.8f, 14);

        center.getWorld().spawnParticle(Particle.EXPLOSION, center, 1);
        Particle.DustOptions demonDust = new Particle.DustOptions(BLADEWEAVER_DEMON, 2.0f);
        center.getWorld().spawnParticle(Particle.DUST, center, 40, 1.5, 1, 1.5, 0, demonDust);

        for (int i = 0; i < 8; i++) {
            double angle = (Math.PI * 2 * i / 8);
            double x = Math.cos(angle) * 1.5;
            double z = Math.sin(angle) * 1.5;
            center.getWorld().spawnParticle(Particle.SWEEP_ATTACK, center.clone().add(x, 0, z), 1);
        }

        center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 1.3f);
        center.getWorld().playSound(center, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.8f);

        for (LivingEntity entity : center.getWorld().getNearbyLivingEntities(afterimageLocation, afterimageDamageRadius)) {
            if (Spellbook.canAttack(caster, entity)) {
                double damage = Spellbook.getVariedAttributeBasedDamage(data, caster, entity, true, Attribute.ADVANTAGE_PHYSICAL, "afterimageDamage");
                double before = entity.getHealth() + entity.getAbsorptionAmount();
                CombatSourceContext.runSpell(this, () -> entity.damage(damage, caster, PDamageType.PHYSICAL));
                if (entity.getHealth() + entity.getAbsorptionAmount() < before) grantBonusHealthForHit();
            }
        }
    }

    private void removeInvincibility() {
        if (invincible) {
            caster.getAttribute(Attribute.RESISTANCE_PHYSICAL).removeModifier(invincibilityModifier);
            caster.getAttribute(Attribute.RESISTANCE_MAGICAL).removeModifier(invincibilityModifier);
            invincible = false;
        }
    }

    @Override
    protected void cleanup() {
        if (afterimageRing != null) {
            afterimageRing.close();
            afterimageRing = null;
        }
        removeInvincibility();
        dashing = false;
        super.cleanup();
    }
}
