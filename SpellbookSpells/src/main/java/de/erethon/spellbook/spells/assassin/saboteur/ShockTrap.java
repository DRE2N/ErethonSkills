package de.erethon.spellbook.spells.assassin.saboteur;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.assassin.AssassinBaseTrap;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.SphereEffect;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ShockTrap extends AssassinBaseTrap {

    private final EffectData stunEffectData = Bukkit.getServer().getSpellbookAPI().getLibrary().getEffectByID("Stun");
    private final EffectData vulnerabilityEffectData = Bukkit.getServer().getSpellbookAPI().getLibrary().getEffectByID("Vulnerability");
    private final int stunDuration = data.getInt("stunDuration", 2);
    private final double bonusDamage = data.getDouble("bonusDamage", 15.0);

    private final Set<UUID> triggeredEntities = new HashSet<>();

    public ShockTrap(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    public boolean onCast() {
        if (!super.onCast()) {
            return false;
        }
        tickInterval = 5;
        return true;
    }

    @Override
    public void onTick() {
        super.onTick();

        boolean playedEffectThisTick = false;
        Set<LivingEntity> newlyTriggered = new HashSet<>();

        for (LivingEntity entity : getEntities()) {
            if (!Spellbook.canAttack(caster, entity)) {
                continue;
            }

            if (triggeredEntities.contains(entity.getUniqueId())) {
                continue;
            }

            double currentDamage = Spellbook.getVariedAttributeBasedDamage(data, caster, entity, false, Attribute.ADVANTAGE_MAGICAL);
            boolean isVulnerable = entity.hasEffect(vulnerabilityEffectData);

            if (isVulnerable) {
                currentDamage += bonusDamage;
            }

            entity.damage(currentDamage, caster);
            entity.addEffect(caster, stunEffectData, stunDuration * 20, 1);

            triggeredEntities.add(entity.getUniqueId());
            newlyTriggered.add(entity);

            if (!playedEffectThisTick) {
                playTriggerVisualEffect(target != null ? target : entity.getLocation());
                playTriggerSoundEffect(target!= null ? target : entity.getLocation());
                playedEffectThisTick = true;
            }
        }
        if (!newlyTriggered.isEmpty()) {
            triggerTraits(newlyTriggered);
        }
    }

    private void playTriggerVisualEffect(Location location) {
        EffectManager effectManager = Spellbook.getInstance().getEffectManager();
        if (effectManager == null) return;

        SphereEffect shockEffect = new SphereEffect(effectManager);
        shockEffect.setLocation(location);
        shockEffect.radius = (float) size * 1.1f;
        shockEffect.particles = 40;
        shockEffect.particle = Particle.CRIT;
        shockEffect.color = Color.YELLOW;
        shockEffect.duration = 6;
        shockEffect.particleCount = 2;
        shockEffect.start();
    }

    private void playTriggerSoundEffect(Location location) {
        location.getWorld().playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 1.8f);
        location.getWorld().playSound(location, Sound.BLOCK_CONDUIT_ACTIVATE, 1.0f, 1.5f);
    }

    @Override
    protected void onTickFinish() {
        super.onTickFinish();
        triggeredEntities.clear();
    }
}