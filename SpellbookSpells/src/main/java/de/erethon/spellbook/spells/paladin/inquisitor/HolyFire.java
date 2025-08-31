package de.erethon.spellbook.spells.paladin.inquisitor;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.slikey.effectlib.effect.CircleEffect;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;

import java.util.HashSet;
import java.util.Set;

public class HolyFire extends InquisitorBaseSpell {

    // Creates an expanding circle of fire that damages enemies and heals allies
    // Does bonus healing based on judgment stacks on the enemies it has dealt damage to

    private final int duration = data.getInt("duration", 10);
    private final float rangeMin = (float) data.getDouble("rangeMin", 0.8);
    private final float rangeMax = (float) data.getDouble("rangeMax", 3.0);
    private final double healAmount = data.getDouble("baseFinishHeal", 15);
    private final double bonusHealPerJudgementStack = data.getDouble("bonusHealPerJudgementStack", 25);

    private CircleEffect circleEffect;
    private float range;

    public HolyFire(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = duration * 20;
        channelDuration = keepAliveTicks;
    }

    @Override
    public boolean onCast() {
        createCircularAoE(caster.getLocation(), rangeMax, 2, keepAliveTicks)
                .onEnter((aoe, entity) -> {
                    if (Spellbook.canAttack(caster, entity)) {
                        double damage = Spellbook.getVariedAttributeBasedDamage(data, caster, entity, false, Attribute.ADVANTAGE_MAGICAL);
                        entity.damage(damage, caster);
                        entity.addEffect(caster, Spellbook.getEffectData("Weakness"), 60, 1);
                        entity.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, entity.getLocation(), 5, 0.3, 0.3, 0.3);
                        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_BLAZE_HURT, 0.8f, 1.2f);
                    } else if (entity != caster) {
                        entity.heal(3.0);
                        entity.addEffect(caster, Spellbook.getEffectData("Regeneration"), 100, 1);
                        entity.getWorld().spawnParticle(Particle.HEART, entity.getLocation().add(0, 2, 0), 2, 0.3, 0.3, 0.3);
                    }
                })
                .onTick(aoe -> {
                    range = (float) Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_MAGICAL, rangeMin, rangeMax, "range");

                    for (LivingEntity entity : aoe.getEntitiesInside()) {
                        if (Spellbook.canAttack(caster, entity)) {
                            entity.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, entity.getLocation(), 1, 0.2, 0.2, 0.2);
                            if (entity.getTicksLived() % 40 == 0) {
                                double tickDamage = Spellbook.getVariedAttributeBasedDamage(data, caster, entity, false, Attribute.ADVANTAGE_MAGICAL) * 0.2;
                                entity.damage(tickDamage, caster);
                                entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 0.3f, 1.5f);
                            }
                        } else if (entity != caster) {
                            entity.getWorld().spawnParticle(Particle.ENCHANTED_HIT, entity.getLocation(), 1, 0.2, 0.2, 0.2);
                            // Continuous healing every 2 seconds
                            if (entity.getTicksLived() % 40 == 0) {
                                entity.heal(1.5);
                                entity.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, entity.getLocation(), 2, 0.2, 0.2, 0.2);
                            }
                        }
                    }
                });

        circleEffect = new CircleEffect(Spellbook.getInstance().getEffectManager());
        circleEffect.particle = Particle.SOUL_FIRE_FLAME;
        circleEffect.duration = keepAliveTicks * 50;
        circleEffect.iterations = -1;
        circleEffect.period = 20;
        circleEffect.particleCount = 32;
        circleEffect.radius = range;
        circleEffect.wholeCircle = true;
        circleEffect.enableRotation = false;
        circleEffect.setLocation(caster.getLocation().clone().add(0, 1, 0));
        circleEffect.start();

        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 1.0f, 0.8f);
        return super.onCast();
    }

    @Override
    protected void onTick() {
        Location loc = caster.getLocation().clone();
        loc.setPitch(0);
        loc.add(0, 1, 0);
        circleEffect.setLocation(loc);

        super.onTick();
    }

    @Override
    protected void onChannelFinish() {
        CircleEffect explode = new CircleEffect(Spellbook.getInstance().getEffectManager());
        explode.particle = Particle.SOUL_FIRE_FLAME;
        explode.duration = 20 * 50;
        explode.iterations = -1;
        explode.particleCount = 64;
        explode.radius = range;
        explode.wholeCircle = true;
        explode.enableRotation = false;
        Location loc = caster.getLocation().clone();
        loc.setPitch(0);
        loc.add(0, 1, 0);
        explode.setLocation(loc);
        explode.start();

        Set<LivingEntity> enemies = new HashSet<>();
        Set<LivingEntity> friends = new HashSet<>();

        loc.getWorld().playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 1.5f, 1.0f);
        loc.getWorld().spawnParticle(Particle.EXPLOSION, loc, 5, 2.0, 1.0, 2.0);

        // Create final explosion effect that deals massive damage/healing
        createCircularAoE(loc, explode.radius, 2, 80)
                .onEnter((aoe, entity) -> {
                    if (!Spellbook.canAttack(caster, entity) && entity != caster) {
                        friends.add(entity);
                        entity.getWorld().spawnParticle(Particle.HEART, entity.getLocation().add(0, 2, 0), 5, 0.5, 0.5, 0.5);
                    } else if (entity != caster) {
                        enemies.add(entity);
                        double finalDamage = Spellbook.getVariedAttributeBasedDamage(data, caster, entity, true, Attribute.ADVANTAGE_MAGICAL);
                        entity.damage(finalDamage, caster);
                        entity.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, entity.getLocation(), 8, 0.5, 0.5, 0.5);
                    }
                })
                .onTick(aoe -> {
                    // Create lingering consecrated/desecrated ground
                    for (LivingEntity entity : aoe.getEntitiesInside()) {
                        if (Spellbook.canAttack(caster, entity)) {
                            if (entity.getTicksLived() % 20 == 0) { // Every second
                                entity.addEffect(caster, Spellbook.getEffectData("Burning"), 40, 1);
                                entity.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, entity.getLocation(), 2, 0.2, 0.2, 0.2);
                            }
                        } else if (entity != caster) {
                            if (entity.getTicksLived() % 20 == 0) { // Every second
                                entity.addEffect(caster, Spellbook.getEffectData("Regeneration"), 40, 1);
                                entity.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, entity.getLocation(), 2, 0.2, 0.2, 0.2);
                            }
                        }
                    }
                });

        triggerTraits(enemies, 1);

        int totalJudgementStacks = 0;
        for (LivingEntity living : enemies) {
            int judgementStacks = getJudgementStacksOnTarget(living);
            totalJudgementStacks += judgementStacks;
            for (int i = 0; i <= judgementStacks; i++) {
                removeJudgement(living);
            }
        }

        double bonusHeal = bonusHealPerJudgementStack * totalJudgementStacks;
        for (LivingEntity living : friends) {
            living.heal(healAmount + bonusHeal + Spellbook.getScaledValue(data, caster, Attribute.STAT_HEALINGPOWER));
            living.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, living.getLocation(), 8, 0.5, 0.5, 0.5);
            living.getWorld().playSound(living.getLocation(), Sound.BLOCK_BEACON_AMBIENT, 0.8f, 1.2f);
        }
    }

    @Override
    protected void cleanup() {
        circleEffect.cancel();
    }

    @Override
    protected void addSpellPlaceholders() {
        spellAddedPlaceholders.add(Component.text(Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_MAGICAL, rangeMin, rangeMax, "range"), VALUE_COLOR));
        placeholderNames.add("range");
    }
}
