package de.erethon.spellbook.spells.assassin.cutthroat;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.assassin.AssassinBaseSpell;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.LineEffect;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BladeDash extends AssassinBaseSpell {

    // The Cutthroat dashes forward, dealing damage to all enemies in its path.
    // Affected enemies are weakened.
    // After dashing, the Cutthroat gains energy based on the number of enemies hit.

    private final double dashDistance = data.getDouble("distance", 6.0);
    private final double dashSpeedMultiplier = data.getDouble("speedMultiplier", 1.8);
    private final double sideDashStrength = data.getDouble("sideDashStrength", 1.5);
    private final double damageWidth = data.getDouble("damageWidth", 1.5);
    private final double energyPerTarget = data.getDouble("energyPerTarget", 5.0);
    private final int weaknessDurationMin = data.getInt("weaknessDurationMin", 20);
    private final int weaknessStacksMin = data.getInt("weaknessStacksMin", 1);
    private final int weaknessDurationMax = data.getInt("weaknessDurationMax", 100);
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
        if (caster instanceof Player player ) {
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

        Set<LivingEntity> affectedTargets = new HashSet<>();
        BoundingBox dashPathBox = BoundingBox.of(startLocation, endLocation).expand(damageWidth / 2.0, 0.5, damageWidth / 2.0);

        for (Entity entity : caster.getWorld().getNearbyEntities(dashPathBox)) {
            if (entity instanceof LivingEntity living && !entity.equals(caster) && Spellbook.canAttack(caster, living)) {
                affected.add((living));
                living.getCollidableExemptions().add(caster.getUniqueId());
                double damage = Spellbook.getVariedAttributeBasedDamage(data, caster, living, true, Attribute.ADVANTAGE_PHYSICAL);
                living.damage(damage, caster);
                int weaknessDuration = (int) Spellbook.getRangedValue(data, caster, living, Attribute.ADVANTAGE_MAGICAL, weaknessDurationMin, weaknessDurationMax, "weaknessDuration");
                int weaknessStacks = (int) Spellbook.getRangedValue(data, caster, living, Attribute.ADVANTAGE_MAGICAL, weaknessStacksMin, weaknessStacksMax, "weaknessStacks");
                living.addEffect(caster, weaknessEffectData, weaknessDuration, weaknessStacks);
                affectedTargets.add(living);
                playHitEffect(living.getEyeLocation());
            }
        }
        for (LivingEntity target : affected) {
            target.getCollidableExemptions().remove(caster.getUniqueId());
        }

        int energyGain = affectedTargets.size() * (int) energyPerTarget;
        caster.setEnergy(caster.getEnergy() + energyGain);

        Vector velocity = direction.multiply(dashSpeedMultiplier);
        caster.setVelocity(velocity);

        playVisualEffect(startLocation, direction);
        playSoundEffect(startLocation);
        triggerTraits(affectedTargets);

        return true;
    }

    private void playVisualEffect(Location start, Vector direction) {
        EffectManager effectManager = Spellbook.getInstance().getEffectManager();
        if (effectManager == null) return;

        LineEffect effect = new LineEffect(effectManager);
        effect.setLocation(start.clone().add(0, 0.5, 0));
        effect.setTarget(start.clone().add(direction.clone().multiply(dashDistance)).add(0, 0.5, 0));
        effect.particle = Particle.CRIT;
        effect.particles = (int) (dashDistance * 5);
        effect.duration = 8;
        effect.start();

        LineEffect trailEffect = new LineEffect(effectManager);
        trailEffect.setLocation(start.clone().add(0, 0.5, 0));
        trailEffect.setTarget(start.clone().add(direction.clone().multiply(dashDistance)).add(0, 0.5, 0));
        trailEffect.particle = Particle.DUST;
        trailEffect.particles = (int) (dashDistance * 3);
        trailEffect.duration = 10;
        trailEffect.start();
    }

    private void playHitEffect(Location hitLocation) {
        hitLocation.getWorld().spawnParticle(Particle.CRIT, hitLocation, 5, 0.2, 0.2, 0.2, 0.1);
    }

    private void playSoundEffect(Location location) {
        location.getWorld().playSound(location, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.5f);
        location.getWorld().playSound(location, Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 1.8f);
    }

    @Override
    protected void addSpellPlaceholders() {
        spellAddedPlaceholders.add(Component.text(Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_MAGICAL, weaknessDurationMin, weaknessDurationMax, "weaknessDuration"), VALUE_COLOR));
        placeholderNames.add("weaknessDuration");
        spellAddedPlaceholders.add(Component.text(Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_MAGICAL, weaknessStacksMin, weaknessStacksMax, "weaknessStacks"), VALUE_COLOR));
        placeholderNames.add("weaknessStacks");
    }

}