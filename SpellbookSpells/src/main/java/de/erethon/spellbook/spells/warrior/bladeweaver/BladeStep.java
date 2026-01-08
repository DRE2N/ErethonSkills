package de.erethon.spellbook.spells.warrior.bladeweaver;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Blade Step - Short range forward dash with brief invincibility.
 * In demon form: Leaves a damaging afterimage with orbiting spectral swords that explodes after a short delay.
 */
public class BladeStep extends BladeweaverBaseSpell {

    private final double dashSpeed = data.getDouble("dashSpeed", 1.1);
    private final int dashDurationTicks = data.getInt("dashDurationTicks", 2);
    private final int invincibilityTicks = data.getInt("invincibilityTicks", 8);
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

    private final List<ItemDisplay> afterimageSwords = new ArrayList<>();

    public BladeStep(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = invincibilityTicks + afterimageDelay + 10;
    }

    @Override
    public boolean onCast() {
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
    protected boolean onPrecast() {
        return super.onPrecast();
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
        Particle.DustOptions dust = new Particle.DustOptions(getThemeColor(), 1.2f);
        effectLoc.getWorld().spawnParticle(Particle.DUST, effectLoc, 30, 0.3, 0.5, 0.3, 0, dust);
        effectLoc.getWorld().spawnParticle(Particle.SWEEP_ATTACK, effectLoc, 5, 0.5, 0.5, 0.5, 0);
    }

    private void spawnArrivalEffect(Location loc) {
        Location effectLoc = loc.clone().add(0, 1, 0);
        Particle.DustOptions dust = new Particle.DustOptions(getThemeColor(), 1.2f);
        effectLoc.getWorld().spawnParticle(Particle.DUST, effectLoc, 20, 0.3, 0.5, 0.3, 0, dust);
        effectLoc.getWorld().spawnParticle(Particle.END_ROD, effectLoc, 10, 0.2, 0.3, 0.2, 0.05);
    }

    private void createAfterimageAnimation() {
        Location center = afterimageLocation.clone().add(0, 1, 0);

        for (int i = 0; i < 3; i++) {
            final int index = i;
            ItemDisplay sword = center.getWorld().spawn(center, ItemDisplay.class, display -> {
                display.setItemStack(new ItemStack(Material.GOLDEN_SWORD));
                display.setBillboard(Display.Billboard.FIXED);
                display.setGlowing(true);
                display.setPersistent(false);
                display.setGlowColorOverride(BLADEWEAVER_DEMON);
                display.setInterpolationDuration(2);

                double angle = (Math.PI * 2 * index / 3);
                float x = (float) (Math.cos(angle) * 1.2);
                float z = (float) (Math.sin(angle) * 1.2);

                Quaternionf rotation = new Quaternionf();
                rotation.rotateY((float) angle);
                rotation.rotateX((float) Math.PI / 4);

                display.setTransformation(new Transformation(
                    new Vector3f(x, 0, z),
                    rotation,
                    new Vector3f(0.8f, 0.8f, 0.8f),
                    new Quaternionf()
                ));
            });
            afterimageSwords.add(sword);
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
        float time = (afterimageDelay - afterimageTimer) * 0.3f;

        for (int i = 0; i < afterimageSwords.size(); i++) {
            ItemDisplay sword = afterimageSwords.get(i);
            if (sword == null || !sword.isValid()) continue;

            double angle = (Math.PI * 2 * i / 3) + time;
            float radius = 1.2f - (afterimageTimer / (float) afterimageDelay) * 0.4f;
            float x = (float) (Math.cos(angle) * radius);
            float z = (float) (Math.sin(angle) * radius);
            float y = (float) Math.sin(time * 2 + i) * 0.2f;

            Location swordLoc = center.clone().add(x, y, z);
            sword.teleport(swordLoc);

            Quaternionf rotation = new Quaternionf();
            rotation.rotateY((float) angle + (float) Math.PI / 2);
            rotation.rotateZ((float) Math.PI / 4);
            rotation.rotateX(time);

            sword.setTransformation(new Transformation(
                new Vector3f(0, 0, 0),
                rotation,
                new Vector3f(0.8f, 0.8f, 0.8f),
                new Quaternionf()
            ));
        }

        if (afterimageTimer % 3 == 0) {
            Particle.DustOptions demonDust = new Particle.DustOptions(BLADEWEAVER_DEMON, 1.0f);
            center.getWorld().spawnParticle(Particle.DUST, center, 8, 0.3, 0.5, 0.3, 0, demonDust);
            center.getWorld().spawnParticle(Particle.FLAME, center, 3, 0.2, 0.3, 0.2, 0.02);
        }
    }

    private void triggerAfterimageExplosion() {
        if (afterimageLocation == null) return;

        Location center = afterimageLocation.clone().add(0, 1, 0);

        for (ItemDisplay sword : afterimageSwords) {
            if (sword != null && sword.isValid()) {
                Location swordLoc = sword.getLocation();
                sword.getWorld().spawnParticle(Particle.FLAME, swordLoc, 5, 0.1, 0.1, 0.1, 0.05);
                sword.remove();
            }
        }
        afterimageSwords.clear();

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
                entity.damage(damage, caster, PDamageType.PHYSICAL);
                grantBonusHealthForHit();
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
        removeInvincibility();
        dashing = false;
        for (ItemDisplay sword : afterimageSwords) {
            if (sword != null && sword.isValid()) {
                sword.remove();
            }
        }
        afterimageSwords.clear();
        super.cleanup();
    }
}
