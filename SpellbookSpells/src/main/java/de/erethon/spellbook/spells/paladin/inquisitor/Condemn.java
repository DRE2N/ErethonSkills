package de.erethon.spellbook.spells.paladin.inquisitor;


import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import net.kyori.adventure.text.Component;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class Condemn extends InquisitorBaseSpell {

    // RMB.
    // Slam down the spear, dealing damage and weakness to all enemies in a cone in front of you.
    // Consumes all judgement on enemies and applies burning for each stack of judgement consumed.
    // Heals allies in a small radius per judgement consumed.

    private final int burningStacksPerJudgement = data.getInt("burningStacksPerJudgement", 1);
    private final int burningDurationMin = data.getInt("burningDurationMin", 4);
    private final int burningDurationMax = data.getInt("burningDurationMax", 8);
    private final int weaknessDurationMin = data.getInt("weaknessDuration", 12) ;
    private final int weaknessDurationMax = data.getInt("weaknessDurationMax", 20);
    private final int weaknessStacks = data.getInt("weaknessStacks", 1);
    private final int healRadiusMin = data.getInt("healRadiusMin", 3);
    private final int healRadiusMax = data.getInt("healRadiusMax", 5);
    private final int healingPerJudgement = data.getInt("healingPerJudgement", 20);

    private final EffectData burning = Spellbook.getEffectData("Burning");
    private final EffectData weakness = Spellbook.getEffectData("Weakness");

    public Condemn(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    public boolean onCast() {
        Vector direction = caster.getEyeLocation().getDirection();

        createConeAoE(caster.getLocation(), 5, 90, 2, direction, 10)
                .onEnter((aoe, entity) -> {
                    if (Spellbook.canAttack(caster, entity)) {
                        double damage = Spellbook.getVariedAttributeBasedDamage(data, caster, entity, true, Attribute.ADVANTAGE_PHYSICAL);
                        entity.damage(damage, caster);

                        int judgementStacks = getJudgementStacksOnTarget(entity);
                        if (judgementStacks > 0) {
                            int stacksToApply = judgementStacks * burningStacksPerJudgement;
                            int burningDuration = (int) Spellbook.getRangedValue(data, caster, entity, Attribute.ADVANTAGE_MAGICAL, burningDurationMin, burningDurationMax, "burningDuration") * 20;
                            int weaknessDuration = (int) Spellbook.getRangedValue(data, caster, entity, Attribute.ADVANTAGE_MAGICAL, weaknessDurationMin, weaknessDurationMax, "weaknessDuration") * 20;

                            entity.addEffect(caster, burning, burningDuration, stacksToApply);
                            entity.addEffect(caster, weakness, weaknessDuration, weaknessStacks);
                            removeJudgement(entity);

                            entity.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, entity.getLocation().add(0, 1, 0), stacksToApply * 3, 0.5, 0.5, 0.5);
                            entity.getWorld().spawnParticle(Particle.SMOKE, entity.getLocation(), 8, 0.3, 0.3, 0.3);
                            entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.0f, 1.2f);
                        }

                        entity.getWorld().spawnParticle(Particle.CRIT, entity.getLocation().add(0, 1, 0), 5, 0.3, 0.5, 0.3);
                        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 0.8f);
                    }
                });

        // Count total judgement consumed for healing calculation
        double totalJudgementConsumed = caster.getLocation().getNearbyLivingEntities(8).stream().filter(entity -> Spellbook.canAttack(caster, entity)).mapToDouble(this::getJudgementStacksOnTarget).sum();

        if (totalJudgementConsumed > 0) {
            caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.5f, 0.8f);
            caster.getWorld().spawnParticle(Particle.EXPLOSION, caster.getLocation().add(0, 0.5, 0), 3, 1.0, 0.5, 1.0);
            caster.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, caster.getLocation(), 15, 2, 0.5, 2);

            int healRadius = (int) Spellbook.getRangedValue(data, caster, null, Attribute.ADVANTAGE_MAGICAL, healRadiusMin, healRadiusMax, "healRadius");

            // Create healing sanctuary that provides regeneration
            createCircularAoE(caster.getLocation(), healRadius, 1, 60)
                    .onEnter((aoe, entity) -> {
                        if (entity != caster && !Spellbook.canAttack(caster, entity)) {
                            double healAmount = (totalJudgementConsumed * healingPerJudgement) + Spellbook.getRangedValue(data, caster, entity, Attribute.STAT_HEALINGPOWER, 5, 25, "healAmount");
                            entity.heal(healAmount);
                            entity.getWorld().spawnParticle(Particle.HEART, entity.getLocation().add(0, 2, 0), 4, 0.5, 0.5, 0.5);
                            entity.getWorld().spawnParticle(Particle.ENCHANTED_HIT, entity.getLocation(), 5, 0.3, 0.5, 0.3);
                            entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_BEACON_AMBIENT, 0.8f, 1.2f);
                        }
                    })
                    .onTick(aoe -> {
                        // Provide continuous regeneration while in sanctuary
                        for (LivingEntity entity : aoe.getEntitiesInside()) {
                            if (entity != caster && !Spellbook.canAttack(caster, entity)) {
                                if (entity.getTicksLived() % 20 == 0) { // Every second
                                    entity.heal(2.0);
                                    entity.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, entity.getLocation(), 2, 0.3, 0.3, 0.3);
                                }
                            }
                        }
                    });

            caster.heal(totalJudgementConsumed * healingPerJudgement);
            caster.getWorld().spawnParticle(Particle.HEART, caster.getLocation().add(0, 2, 0), 4, 0.5, 0.5, 0.5);
            caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_BEACON_AMBIENT, 1.0f, 1.0f);
        }

        return super.onCast();
    }

    @Override
    protected void addSpellPlaceholders() {
        spellAddedPlaceholders.add(Component.text(Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_MAGICAL, burningDurationMin, burningDurationMax, "burningDuration"), VALUE_COLOR));
        placeholderNames.add("burningDuration");
        spellAddedPlaceholders.add(Component.text(Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_MAGICAL, weaknessDurationMin, weaknessDurationMax, "weaknessDuration"), VALUE_COLOR));
        placeholderNames.add("weaknessDuration");
        spellAddedPlaceholders.add(Component.text(Spellbook.getRangedValue(data, caster, Attribute.STAT_HEALINGPOWER, healRadiusMin, healRadiusMax, "healRadius"), VALUE_COLOR));
        placeholderNames.add("healRadius");
    }
}
