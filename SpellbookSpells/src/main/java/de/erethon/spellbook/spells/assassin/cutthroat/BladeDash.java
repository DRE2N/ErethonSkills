package de.erethon.spellbook.spells.assassin.cutthroat;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.assassin.AssassinBaseSpell;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.LineEffect;
import de.slikey.effectlib.effect.SphereEffect;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

public class BladeDash extends CutthroatBaseSpell {

    // The Cutthroat dashes forward, dealing damage to all enemies in its path.
    // Affected enemies are weakened.
    // After dashing, the Cutthroat gains energy based on the number of enemies hit.

    private final double dashDistance = data.getDouble("distance", 6.0);
    private final double dashSpeedMultiplier = data.getDouble("speedMultiplier", 1.8);
    private final double sideDashStrength = data.getDouble("sideDashStrength", 1.5);
    private final double damageWidth = data.getDouble("damageWidth", 1.5);
    private final double energyPerTarget = data.getDouble("energyPerTarget", 5.0);
    private final int weaknessDurationMin = data.getInt("weaknessDurationMin", 2) * 20;
    private final int weaknessStacksMin = data.getInt("weaknessStacksMin", 1);
    private final int weaknessDurationMax = data.getInt("weaknessDurationMax", 5) * 20;
    private final int weaknessStacksMax = data.getInt("weaknessStacksMax", 3);

    private final EffectData weaknessEffectData = Spellbook.getEffectData("Weakness");
    private final Set<LivingEntity> affected = new HashSet<>();

    public BladeDash(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    public boolean onCast() {
        if (!super.onCast()) {
            return false;
        }

        Location location = caster.getLocation();
        location.setPitch(-10);
        Vector direction = location.getDirection().normalize();
        Vector inputOffset = new Vector();
        // Add dash to the left/right based on player input
        if (caster instanceof Player player) {
            Input input = player.getCurrentInput();
            Vector right = direction.clone().crossProduct(new Vector(0, 1, 0)).normalize();
            if (input.isLeft()) {
                inputOffset.add(right.clone().multiply(-sideDashStrength));
            }
            if (input.isRight()) {
                inputOffset.add(right.clone().multiply(sideDashStrength));
            }
        }
        Vector forwardDash = direction.multiply(dashDistance);
        // Combine forward dash and side dash (inputOffset)
        Vector dashVector = forwardDash.add(inputOffset);
        Location startLocation = location.clone().add(0, 0.5, 0);
        Location endLocation = startLocation.clone().add(dashVector);

        playDashWindup(startLocation, direction);
        executeDash(startLocation, endLocation, dashVector, direction);

        return true;
    }

    private void playDashWindup(Location startLocation, Vector direction) {
        caster.getWorld().spawnParticle(Particle.SMOKE, startLocation, 10, 0.3, 0.3, 0.3, 0.05);
        caster.getWorld().spawnParticle(Particle.ENCHANTED_HIT, startLocation, 8, 0.2, 0.2, 0.2, 0.1);
        caster.getWorld().playSound(startLocation, Sound.ENTITY_ENDERMAN_TELEPORT, 0.3f, 2.0f);

        EffectManager effectManager = Spellbook.getInstance().getEffectManager();
        if (effectManager != null) {
            SphereEffect chargeEffect = new SphereEffect(effectManager);
            chargeEffect.setLocation(startLocation);
            chargeEffect.radius = 1.0f;
            chargeEffect.particle = Particle.DUST;
            chargeEffect.particles = 15;
            chargeEffect.duration = 5;
            chargeEffect.start();
        }
    }

    private void executeDash(Location startLocation, Location endLocation, Vector dashVector, Vector direction) {
        Set<LivingEntity> affectedTargets = new HashSet<>();
        BoundingBox dashPathBox = BoundingBox.of(startLocation, endLocation).expand(damageWidth / 2.0, 0.5, damageWidth / 2.0);

        for (Entity entity : caster.getWorld().getNearbyEntities(dashPathBox)) {
            if (entity instanceof LivingEntity living && !entity.equals(caster) && Spellbook.canAttack(caster, living)) {
                affected.add(living);
                living.getCollidableExemptions().add(caster.getUniqueId());

                double damage = Spellbook.getVariedAttributeBasedDamage(data, caster, living, true, Attribute.ADVANTAGE_PHYSICAL);
                living.damage(damage, caster);

                int weaknessDuration = (int) Spellbook.getRangedValue(data, caster, living, Attribute.ADVANTAGE_MAGICAL, weaknessDurationMin, weaknessDurationMax, "weaknessDuration");
                int weaknessStacks = (int) Spellbook.getRangedValue(data, caster, living, Attribute.ADVANTAGE_MAGICAL, weaknessStacksMin, weaknessStacksMax, "weaknessStacks");
                living.addEffect(caster, weaknessEffectData, weaknessDuration, weaknessStacks);

                affectedTargets.add(living);
                playSlashEffect(living.getLocation());
            }
        }

        for (LivingEntity target : affected) {
            target.getCollidableExemptions().remove(caster.getUniqueId());
        }

        int energyGain = affectedTargets.size() * (int) energyPerTarget;
        caster.setEnergy(caster.getEnergy() + energyGain);

        Vector velocity = direction.multiply(dashSpeedMultiplier);
        caster.setVelocity(velocity);

        createDashTrail(startLocation, endLocation);
        playDashSounds(startLocation);
        createDashImpact(endLocation, affectedTargets.size());
        triggerTraits(affectedTargets);
    }

    private void createDashTrail(Location start, Location end) {
        EffectManager effectManager = Spellbook.getInstance().getEffectManager();
        if (effectManager != null) {
            LineEffect slashTrail = new LineEffect(effectManager);
            slashTrail.setLocation(start);
            slashTrail.setTarget(end);
            slashTrail.particle = Particle.SWEEP_ATTACK;
            slashTrail.particles = (int) (dashDistance * 4);
            slashTrail.duration = 15;
            slashTrail.start();

            LineEffect energyTrail = new LineEffect(effectManager);
            energyTrail.setLocation(start);
            energyTrail.setTarget(end);
            energyTrail.particle = Particle.DUST;
            energyTrail.particles = (int) (dashDistance * 6);
            energyTrail.duration = 20;
            energyTrail.start();
        }

        new BukkitRunnable() {
            int ticks = 0;
            final double totalDistance = start.distance(end);

            @Override
            public void run() {
                if (ticks >= 15) {
                    this.cancel();
                    return;
                }

                double progress = ticks / 15.0;
                Location currentPos = start.clone().add(end.clone().subtract(start).multiply(progress));

                currentPos.getWorld().spawnParticle(Particle.DUST, currentPos, 2, 0.1, 0.1, 0.1, 0,
                    new Particle.DustOptions(org.bukkit.Color.SILVER, 1.0f));

                ticks++;
            }
        }.runTaskTimer(Spellbook.getInstance().getImplementer(), 0L, 1L);
    }

    private void playSlashEffect(Location hitLocation) {
        Location adjustedLoc = hitLocation.add(0, 1, 0);
        adjustedLoc.getWorld().spawnParticle(Particle.CRIT, adjustedLoc, 8, 0.3, 0.3, 0.3, 0.2);
        adjustedLoc.getWorld().spawnParticle(Particle.SWEEP_ATTACK, adjustedLoc, 3, 0.2, 0.2, 0.2, 0.1);
        adjustedLoc.getWorld().spawnParticle(Particle.DUST, adjustedLoc, 5, 0.2, 0.2, 0.2, 0,
            new Particle.DustOptions(org.bukkit.Color.RED, 1.2f));

        adjustedLoc.getWorld().playSound(adjustedLoc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.6f, 1.4f);
    }

    private void createDashImpact(Location endLocation, int targetsHit) {
        endLocation.getWorld().spawnParticle(Particle.EXPLOSION, endLocation, 1, 0, 0, 0, 0);
        endLocation.getWorld().spawnParticle(Particle.DUST, endLocation, 15, 1.0, 0.5, 1.0, 0.1,
            new Particle.DustOptions(org.bukkit.Color.GRAY, 1.5f));

        if (targetsHit > 0) {
            endLocation.getWorld().spawnParticle(Particle.BLOCK, endLocation, 20, 0.8, 0.8, 0.8, 0.1,
                Material.REDSTONE_BLOCK.createBlockData());
            endLocation.getWorld().playSound(endLocation, Sound.ENTITY_GENERIC_EXPLODE, 0.3f, 1.8f);
        }

        EffectManager effectManager = Spellbook.getInstance().getEffectManager();
        if (effectManager != null) {
            SphereEffect impactWave = new SphereEffect(effectManager);
            impactWave.setLocation(endLocation);
            impactWave.radius = 2.0f;
            impactWave.particle = Particle.DUST;
            impactWave.particles = 25;
            impactWave.duration = 10;
            impactWave.start();
        }
    }

    private void playDashSounds(Location location) {
        location.getWorld().playSound(location, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.2f, 1.2f);
        location.getWorld().playSound(location, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.5f);
        location.getWorld().playSound(location, Sound.ITEM_TRIDENT_THROW, 0.6f, 1.8f);
    }

    @Override
    protected void addSpellPlaceholders() {
        spellAddedPlaceholders.add(Component.text(Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_MAGICAL, weaknessDurationMin, weaknessDurationMax, "weaknessDuration") / 20, VALUE_COLOR));
        placeholderNames.add("weaknessDuration");
        spellAddedPlaceholders.add(Component.text(Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_MAGICAL, weaknessStacksMin, weaknessStacksMax, "weaknessStacks"), VALUE_COLOR));
        placeholderNames.add("weaknessStacks");
    }

}