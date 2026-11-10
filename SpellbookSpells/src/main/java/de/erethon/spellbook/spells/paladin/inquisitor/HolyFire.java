package de.erethon.spellbook.spells.paladin.inquisitor;
import de.erethon.spellbook.utils.TargetedParticles;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.combat.HealingContext;
import de.erethon.spellbook.spells.vfx.SkillSignatureVfx;
import de.erethon.spellbook.spells.vfx.TerrainRing;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HolyFire extends InquisitorBaseSpell {

    // Creates an expanding circle of fire that damages enemies and heals allies
    // Does bonus healing based on judgment stacks on the enemies it has dealt damage to

    private final int duration = data.getInt("duration", 10);
    private final float rangeMin = (float) data.getDouble("rangeMin", 0.8);
    private final float rangeMax = (float) data.getDouble("rangeMax", 3.0);
    private final double healAmount = data.getDouble("baseFinishHeal", 15);
    private final double bonusHealPerJudgementStack = data.getDouble("bonusHealPerJudgementStack", 25);

    private float range;
    private int visualTicks;
    private TerrainRing boundary;

    public HolyFire(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = duration * 20;
        channelDuration = keepAliveTicks;
    }

    @Override
    public boolean onCast() {
        range = (float) Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_MAGICAL,
                rangeMin, rangeMax, "range");
        createCircularAoE(caster.getLocation(), rangeMax, 2, keepAliveTicks)
                .onEnter((aoe, entity) -> {
                    if (Spellbook.canAttack(caster, entity)) {
                        double damage = Spellbook.getVariedAttributeBasedDamage(data, caster, entity, false, Attribute.ADVANTAGE_MAGICAL);
                        entity.damage(damage, caster);
                        entity.addEffect(caster, Spellbook.getEffectData("Weakness"), 60, 1);
                        SkillSignatureVfx.accent(entity.getLocation().add(0, 1.1, 0), getClass(),
                                "wake", 1.8f, 16, false);
                        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_BLAZE_HURT, 0.8f, 1.2f);
                    } else if (entity != caster) {
                        entity.addEffect(caster, Spellbook.getEffectData("Regeneration"), 100, 1);
                        TargetedParticles.spawnForOthers(Particle.HEART, entity, entity.getLocation().add(0, 2, 0), 2, 0.3, 0.3, 0.3);
                    }
                })
                .onTick(aoe -> {
                    for (LivingEntity entity : aoe.getEntitiesInside()) {
                        if (Spellbook.canAttack(caster, entity)) {
                            entity.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, entity.getLocation(), 1, 0.2, 0.2, 0.2);
                            if (Bukkit.getCurrentTick() % 40 == 0) {
                                double tickDamage = Spellbook.getVariedAttributeBasedDamage(data, caster, entity, false, Attribute.ADVANTAGE_MAGICAL) * 0.2;
                                entity.damage(tickDamage, caster);
                                entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 0.3f, 1.5f);
                            }
                        } else if (entity != caster) {
                            entity.getWorld().spawnParticle(Particle.ENCHANTED_HIT, entity.getLocation(), 1, 0.2, 0.2, 0.2);
                            // Continuous healing every 2 seconds
                            if (Bukkit.getCurrentTick() % 40 == 0) {
                                TargetedParticles.spawnForOthers(Particle.HAPPY_VILLAGER, entity, entity.getLocation(), 2, 0.2, 0.2, 0.2);
                            }
                        }
                    }
                });

        boundary = new TerrainRing(caster.getLocation(), range,
                "terrain_band_inquisitor", 36, 0.42f);
        SkillSignatureVfx.accent(caster.getLocation(), getClass(), "lead", range * 2.0f, 24, true);

        // The cast is useful to a solo Inquisitor even if the channel is interrupted.
        double healingPower = Math.min(100, Math.max(0,
                caster.getAttribute(Attribute.STAT_HEALINGPOWER).getValue()));
        double immediate = Math.min(caster.getMaxHealth() * 0.16, 8 + healingPower * 0.2);
        caster.heal(HealingContext.isPlayerCombat(caster, caster) ? immediate * 0.65 : immediate);

        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 1.0f, 0.8f);
        return super.onCast();
    }

    @Override
    protected void onTick() {
        if (boundary != null && ++visualTicks % 3 == 0) boundary.move(caster.getLocation());

        super.onTick();
    }

    @Override
    protected void onChannelFinish() {
        Location loc = caster.getLocation().clone();
        loc.setPitch(0);
        loc.add(0, 1, 0);
        SkillSignatureVfx.accent(loc, getClass(), "main", range * 2.3f, 26, true);

        Set<LivingEntity> enemies = new HashSet<>();
        Set<LivingEntity> friends = new HashSet<>();
        friends.add(caster);

        loc.getWorld().playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 1.5f, 1.0f);
        SkillSignatureVfx.accent(loc.clone().add(0, 1.2, 0), getClass(),
                "wake", range * 2.0f, 26, false);

        // Resolve the impact now; AoE enter callbacks run on a later manager tick.
        for (LivingEntity entity : loc.getWorld().getNearbyLivingEntities(loc, range, 2, range)) {
            double dx = entity.getX() - loc.getX();
            double dz = entity.getZ() - loc.getZ();
            if (entity == caster || dx * dx + dz * dz > range * range) {
                continue;
            }
            if (Spellbook.canAttack(caster, entity)) {
                enemies.add(entity);
                double finalDamage = Spellbook.getVariedAttributeBasedDamage(data, caster, entity, true, Attribute.ADVANTAGE_MAGICAL);
                entity.damage(finalDamage, caster);
                SkillSignatureVfx.accent(entity.getLocation().add(0, 1.1, 0), getClass(),
                        "wake", 2.4f, 20, false);
            } else {
                friends.add(entity);
                TargetedParticles.spawnForOthers(Particle.HEART, entity, entity.getLocation().add(0, 2, 0), 5, 0.5, 0.5, 0.5);
            }
        }

        createCircularAoE(loc, range, 2, 80)
                .onTick(aoe -> {
                    // Create lingering consecrated/desecrated ground
                    for (LivingEntity entity : aoe.getEntitiesInside()) {
                        if (Spellbook.canAttack(caster, entity)) {
                            if (Bukkit.getCurrentTick() % 20 == 0) { // Every second
                                entity.addEffect(caster, Spellbook.getEffectData("Burning"), 40, 1);
                                entity.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, entity.getLocation(), 2, 0.2, 0.2, 0.2);
                            }
                        } else if (entity != caster) {
                            if (Bukkit.getCurrentTick() % 20 == 0) { // Every second
                                entity.addEffect(caster, Spellbook.getEffectData("Regeneration"), 40, 1);
                                TargetedParticles.spawnForOthers(Particle.HAPPY_VILLAGER, entity, entity.getLocation(), 2, 0.2, 0.2, 0.2);
                            }
                        }
                    }
                });

        triggerTraits(enemies, 1);

        int totalJudgementStacks = 0;
        for (LivingEntity living : enemies) {
            int judgementStacks = getJudgementStacksOnTarget(living);
            totalJudgementStacks += judgementStacks;
            for (int i = 0; i < judgementStacks; i++) {
                removeJudgement(living);
            }
        }

        double bonusHeal = bonusHealPerJudgementStack * Math.min(3, totalJudgementStacks);
        for (LivingEntity living : friends) {
            double heal = Math.min(living.getMaxHealth() * 0.3,
                    healAmount + bonusHeal + Math.min(100, Math.max(0,
                            caster.getAttribute(Attribute.STAT_HEALINGPOWER).getValue())) * 0.25);
            living.heal(HealingContext.isPlayerCombat(caster, living) ? heal * 0.65 : heal);
            TargetedParticles.spawnForOthers(Particle.HAPPY_VILLAGER, living, living.getLocation(), 8, 0.5, 0.5, 0.5);
            living.getWorld().playSound(living.getLocation(), Sound.BLOCK_BEACON_AMBIENT, 0.8f, 1.2f);
        }
    }

    @Override
    protected void cleanup() {
        if (boundary != null) boundary.close();
        super.cleanup();
    }

    @Override
    protected void addSpellPlaceholders() {
        spellAddedPlaceholders.add(Component.text(Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_MAGICAL, rangeMin, rangeMax, "range"), VALUE_COLOR));
        placeholderNames.add("range");
    }
}
