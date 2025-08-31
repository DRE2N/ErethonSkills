package de.erethon.spellbook.spells.paladin.inquisitor;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

public class SpearThrust extends InquisitorBaseSpell {

    // Pushes the target away and applies weakness as well as judgement.
    // If the target has more than 3 stacks of judgement, they are additionally slowed and burnt.
    // Creates a spear thrust line effect and enhanced visual feedback.

    private final double velocity = data.getDouble("velocity", 1.5);
    private final int weaknessDuration = data.getInt("weaknessDuration", 12);
    private final int weaknessStacksMin = data.getInt("weaknessStacksMin", 1);
    private final int weaknessStacksMax = data.getInt("weaknessStacksMax", 3);
    private final int slowDuration = data.getInt("slowDuration", 12);
    private final int burnDuration = data.getInt("burnDuration", 12) ;
    private final int burnStacksMin = data.getInt("burnStacksMin", 1);
    private final int burnStacksMax = data.getInt("burnStacksMax", 3);
    public int minimumJudgementStacks = data.getInt("minimumJudgementStacks", 3); // Trait: Not yet

    private final EffectData weaknessEffect = Spellbook.getEffectData("Weakness");
    private final EffectData burnEffect = Spellbook.getEffectData("Burning");
    private final EffectData slowEffect = Spellbook.getEffectData("Slow");

    public SpearThrust(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onPrecast() {
        return lookForTarget(3);
    }

    @Override
    public boolean onCast() {
        Vector direction = caster.getEyeLocation().getDirection();
        direction.multiply(velocity);
        direction.setY(Math.max(0.2, direction.getY()));
        target.setVelocity(direction);

        Location thrustStart = caster.getEyeLocation();
        Location thrustEnd = target.getLocation().add(0, 1, 0);

        for (double i = 0; i <= 1; i += 0.1) {
            Location particleLoc = thrustStart.clone().add(thrustEnd.clone().subtract(thrustStart).multiply(i));
            caster.getWorld().spawnParticle(Particle.CRIT, particleLoc, 2, 0.1, 0.1, 0.1);
            caster.getWorld().spawnParticle(Particle.SWEEP_ATTACK, particleLoc, 1);
        }

        target.getWorld().spawnParticle(Particle.EXPLOSION, target.getLocation().add(0, 1, 0), 1);
        target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 8, 0.5, 0.5, 0.5);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.0f);
        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.8f, 1.2f);

        int weaknessStacks = (int) Spellbook.getRangedValue(data, caster, target, Attribute.ADVANTAGE_MAGICAL, weaknessStacksMin, weaknessStacksMax, "weaknessStacks");
        target.addEffect(caster, weaknessEffect, weaknessDuration * 20, weaknessStacks);
        addJudgement(target);

        if (getJudgementStacksOnTarget(target) > minimumJudgementStacks) {
            target.addEffect(caster, slowEffect, slowDuration * 20, 1);
            int burnStacks = (int) Spellbook.getRangedValue(data, caster, target, Attribute.ADVANTAGE_MAGICAL, burnStacksMin, burnStacksMax, "burnStacks");
            target.addEffect(caster, burnEffect, burnDuration * 20, burnStacks);

            createCircularAoE(target.getLocation(), 3, 1, 120)
                    .onEnter((aoe, entity) -> {
                        if (Spellbook.canAttack(caster, entity) && entity != target) {
                            entity.addEffect(caster, slowEffect, 60, 1);
                            entity.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, entity.getLocation(), 5, 0.3, 0.3, 0.3);
                            entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_BLAZE_HURT, 0.5f, 1.5f);
                        }
                    })
                    .onTick(aoe -> {
                        for (LivingEntity entity : aoe.getEntitiesInside()) {
                            if (Spellbook.canAttack(caster, entity)) {
                                if (entity.getTicksLived() % 40 == 0) { // Every 2 seconds
                                    double punishmentDamage = Spellbook.getVariedAttributeBasedDamage(data, caster, entity, false, Attribute.ADVANTAGE_MAGICAL) * 0.3;
                                    entity.damage(punishmentDamage, caster);
                                    entity.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, entity.getLocation(), 2, 0.3, 0.3, 0.3);
                                }
                            }
                        }
                    });

            target.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, target.getLocation(), 15, 0.8, 0.5, 0.8);
            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.0f, 1.0f);
            target.getWorld().playSound(target.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 0.5f, 2.0f);
        }

        triggerTraits(target);
        return super.onCast();
    }

    @Override
    protected void addSpellPlaceholders() {
        spellAddedPlaceholders.add(Component.text(Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_MAGICAL, weaknessStacksMin, weaknessStacksMax, "weaknessStacks")));
        placeholderNames.add("weaknessStacks");
        spellAddedPlaceholders.add(Component.text(Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_MAGICAL, burnStacksMin, burnStacksMax, "burnStacks")));
        placeholderNames.add("burnStacks");
    }
}
