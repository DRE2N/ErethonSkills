package de.erethon.spellbook.spells.assassin.cutthroat;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.assassin.AssassinBaseSpell;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.SphereEffect;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WhirlingBlades extends AssassinBaseSpell {

    // The Cutthroat spins around, dealing damage to all enemies in a radius.
    // If the enemy is bleeding, the damage is increased and the bleeding effect is spread to nearby enemies.

    private final double radius = data.getDouble("radius", 3);
    private final double bonusDamageMultiplier = data.getDouble("bonusDamageMultiplier", 1.5);
    private final double bleedingSpreadRadius = data.getDouble("bleedingSpreadRadius", 2);
    private final int bleedingMinDuration = data.getInt("bleedingMinDuration", 5);
    private final int bleedingMaxDuration = data.getInt("bleedingMaxDuration", 10);
    private final EffectData bleedEffectIdentifier = Spellbook.getEffectData("Bleeding");

    public WhirlingBlades(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    public boolean onCast() {
        if (!super.onCast()) {
            return false;
        }

        Location center = caster.getLocation();
        Set<LivingEntity> affectedTargets = new HashSet<>();
        int bleedingDuration = (int) Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_MAGICAL, bleedingMinDuration, bleedingMaxDuration, "bleedingDuration");
        List<Entity> nearbyEntities = caster.getNearbyEntities(radius, radius, radius);
        for (Entity entity : nearbyEntities) {
            if (entity instanceof LivingEntity target && !entity.equals(caster) && Spellbook.canAttack(caster, (LivingEntity) entity)) {
                double damage = Spellbook.getVariedAttributeBasedDamage(data, caster, target, true, Attribute.ADVANTAGE_PHYSICAL);
                boolean isBleeding = caster.hasEffect(bleedEffectIdentifier);
                if (isBleeding) {
                    damage *= bonusDamageMultiplier;
                    // Spread the bleeding effect to nearby targets
                    Collection<Entity> nearbyBleedingEntities = target.getLocation().getNearbyEntities(bleedingSpreadRadius, bleedingSpreadRadius, bleedingSpreadRadius);
                    for (Entity bleedingEntity : nearbyBleedingEntities) {
                        if (bleedingEntity instanceof LivingEntity bleedingTarget && !bleedingTarget.equals(caster) && Spellbook.canAttack(caster, bleedingTarget)) {
                            bleedingTarget.addEffect(caster, bleedEffectIdentifier, bleedingDuration,1);
                            caster.getWorld().spawnParticle(Particle.DUST , bleedingTarget.getLocation(), 3, 0.5, 0.5, 0.5, 0);
                        }
                    }
                }
                target.damage(damage, caster);
                affectedTargets.add(target);
            }
        }

        playVisualEffect(center);
        playSoundEffect(center);
        triggerTraits(affectedTargets);

        return true;
    }

    private void playVisualEffect(Location center) {
        EffectManager effectManager = Spellbook.getInstance().getEffectManager();
        if (effectManager == null) return;

        SphereEffect effect = new SphereEffect(effectManager);
        effect.setLocation(center.add(0, 1, 0));
        effect.radius = (float) radius;
        effect.particles = 50;
        effect.particle = Particle.SWEEP_ATTACK;
        effect.duration = 5;
        effect.particleCount = 1;
        effect.start();

        SphereEffect dustEffect = new SphereEffect(effectManager);
        dustEffect.setLocation(center.add(0, 1, 0));
        dustEffect.radius = (float) radius * 0.8f;
        dustEffect.particles = 30;
        dustEffect.particle = Particle.DUST;
        dustEffect.duration = 5;
        dustEffect.particleCount = 1;
        dustEffect.start();
    }

    private void playSoundEffect(Location location) {
        location.getWorld().playSound(location, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.2f, 0.8f);
        location.getWorld().playSound(location, Sound.ITEM_TRIDENT_RETURN, 0.5f, 1.5f);
    }
}