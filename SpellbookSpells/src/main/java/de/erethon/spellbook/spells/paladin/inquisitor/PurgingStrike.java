package de.erethon.spellbook.spells.paladin.inquisitor;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellEffect;
import net.kyori.adventure.text.Component;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;

import java.util.HashSet;
import java.util.Set;

public class PurgingStrike extends InquisitorBaseSpell {

    // Strike an enemy with your spear, removing a positive effect from them per stack of Judgement.
    // The effect is removed from the target and applied to the caster.
    // Additionally, the target is stunned per stack of Judgement.

    private final int range = data.getInt("range", 3);
    private final int baseStunDuration =  data.getInt("baseStunDuration", 20);
    private final int effectDurationMin = data.getInt("stunDurationMin", 1);
    private final int effectDurationMax = data.getInt("stunDurationMax", 4) ;

    private final EffectData stun = Spellbook.getEffectData("Stun");

    public PurgingStrike(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onPrecast() {
        return super.onPrecast() && lookForTarget(range);
    }

    @Override
    public boolean onCast() {
        Set<EffectData> effects = new HashSet<>();
        for (SpellEffect effect : target.getEffects()) {
            if (effect.data.isPositive()) {
                effects.add(effect.data);
            }
        }

        int stacks = getJudgementStacksOnTarget(target);

        target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 8, 0.5, 0.5, 0.5);
        target.getWorld().spawnParticle(Particle.SWEEP_ATTACK, target.getLocation(), 3, 0.3, 0.3, 0.3);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.0f);

        if (stacks > 0) {
            createCircularAoE(target.getLocation(), 4, 1, 80)
                    .onEnter((aoe, entity) -> {
                        if (Spellbook.canAttack(caster, entity) && entity != target) {
                            for (SpellEffect effect : entity.getEffects()) {
                                if (effect.data.isPositive()) {
                                    entity.removeEffect(effect.data);
                                    int effectDuration = (int) Spellbook.getRangedValue(data, caster, entity, Attribute.ADVANTAGE_MAGICAL, effectDurationMin, effectDurationMax, "stunDuration") * 20;
                                    caster.addEffect(entity, effect.data, effectDuration, 1);

                                    entity.getWorld().spawnParticle(Particle.SMOKE, entity.getLocation().add(0, 1, 0), 5, 0.3, 0.3, 0.3);
                                    caster.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, caster.getLocation().add(0, 1, 0), 3, 0.3, 0.3, 0.3);
                                    entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_WANDERING_TRADER_DISAPPEARED, 0.7f, 1.0f);
                                    break;
                                }
                            }
                        }
                    })
                    .onTick(aoe -> {
                        for (LivingEntity entity : aoe.getEntitiesInside()) {
                            if (Spellbook.canAttack(caster, entity)) {
                                entity.getWorld().spawnParticle(Particle.ENCHANTED_HIT, entity.getLocation(), 1, 0.3, 0.3, 0.3);
                                if (entity.getTicksLived() % 40 == 0) {
                                    entity.addEffect(caster, Spellbook.getEffectData("Weakness"), 40, 1);
                                    entity.getWorld().spawnParticle(Particle.SMOKE, entity.getLocation(), 3, 0.2, 0.2, 0.2);
                                }
                            }
                        }
                    });

        for (int i = 0; i < stacks; i++) {
            if (effects.isEmpty()) break;
            EffectData effect = effects.iterator().next();
            effects.remove(effect);
            target.removeEffect(effect);

            int effectDuration = (int) Spellbook.getRangedValue(data, caster, target, Attribute.ADVANTAGE_MAGICAL, effectDurationMin, effectDurationMax, "stunDuration") * 20;
            caster.addEffect(target, effect, effectDuration, 1);

            // Visual feedback for each effect stolen
            target.getWorld().spawnParticle(Particle.SMOKE, target.getLocation().add(0, 1, 0), 5, 0.3, 0.3, 0.3);
            caster.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, caster.getLocation().add(0, 1, 0), 3, 0.3, 0.3, 0.3);

            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_WANDERING_TRADER_DISAPPEARED, 0.7f, 1.0f);
            caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);

            triggerTraits(target);
        }

        // Enhanced stun effect based on judgement stacks
        int stunDuration = stacks * baseStunDuration;
        target.addEffect(caster, stun, stunDuration, 1);

        // Visual feedback for stun
        target.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, target.getLocation().add(0, 1, 0), stacks * 3, 0.5, 0.5, 0.5);
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_CONDUIT_DEACTIVATE, 1.0f, 0.8f);
    }

    return super.onCast();
}

    @Override
    protected void addSpellPlaceholders() {
        spellAddedPlaceholders.add(Component.text(Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_MAGICAL, effectDurationMin, effectDurationMax, "stunDuration")));
        placeholderNames.add("stunDuration");
    }
}
