package de.erethon.spellbook.spells.assassin.cutthroat;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.assassin.AssassinBaseSpell;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.SphereEffect;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WhirlingBlades extends AssassinBaseSpell {

    private final double radius = data.getDouble("radius", 3);
    private final double bonusDamageMultiplier = data.getDouble("bonusDamageMultiplier", 1.5);
    private final EffectData bleedEffectIdentifier = Bukkit.getServer().getSpellbookAPI().getLibrary().getEffectByID("Bleeding");

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

        List<Entity> nearbyEntities = caster.getNearbyEntities(radius, radius, radius);
        for (Entity entity : nearbyEntities) {
            if (entity instanceof LivingEntity target && !entity.equals(caster) && Spellbook.canAttack(caster, (LivingEntity) entity)) {
                double damage = Spellbook.getVariedAttributeBasedDamage(data, caster, target, true, Attribute.ADVANTAGE_PHYSICAL);
                boolean isBleeding = caster.hasEffect(bleedEffectIdentifier);
                if (isBleeding) {
                    damage *= bonusDamageMultiplier;
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