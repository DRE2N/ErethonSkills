package de.erethon.spellbook.spells.paladin.inquisitor;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.LineEffect;
import net.kyori.adventure.text.Component;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.Location;

public class HolySmite extends InquisitorBaseSpell {

    // Deliver a powerful overhead blow to a single target. Deals moderate physical damage.
    // Consumes all 'Judgement' stacks on the target for significantly increased magic damage per stack.
    // Grants Fury per stack consumed.

    private final int range = data.getInt("range", 10);
    private final int bonusDamagePerStack = data.getInt("bonusDamagePerStack", 25);
    private final int furyStacksPerStack = data.getInt("furyStacksPerStack", 1);
    private final int furyDurationMin = data.getInt("furyDurationMin", 5);
    private final int furyDurationMax = data.getInt("furyDurationMax", 15);

    private final EffectManager effectManager = Spellbook.getInstance().getEffectManager();
    private final EffectData fury = Spellbook.getEffectData("Fury");

    public HolySmite(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onPrecast() {
        return super.onPrecast() && lookForTarget(range);
    }

    @Override
    public boolean onCast() {
        int judgementStacks = getJudgementStacksOnTarget(target);

        Location strikeLocation = target.getLocation().add(0, 15, 0);
        for (int i = 0; i < 15; i++) {
            Location particleLoc = strikeLocation.clone().subtract(0, i, 0);
            caster.getWorld().spawnParticle(Particle.WHITE_ASH, particleLoc, 2, 0.1, 0.1, 0.1);
            caster.getWorld().spawnParticle(Particle.ENCHANTED_HIT, particleLoc, 1, 0.05, 0.05, 0.05);
        }

        createCircularAoE(target.getLocation(), 3, 1, 100)
                .onEnter((aoe, entity) -> {
                    if (entity == target) {
                        entity.getWorld().spawnParticle(Particle.EXPLOSION, entity.getLocation(), 2, 0.5, 0.5, 0.5);
                        entity.getWorld().spawnParticle(Particle.FLASH, entity.getLocation(), 5, 0.3, 0.3, 0.3);
                    } else if (Spellbook.canAttack(caster, entity)) {
                        double consecrationDamage = Spellbook.getVariedAttributeBasedDamage(data, caster, entity, false, Attribute.ADVANTAGE_MAGICAL) * 0.4;
                        entity.damage(consecrationDamage, caster);
                        entity.getWorld().spawnParticle(Particle.WHITE_ASH, entity.getLocation(), 5, 0.3, 0.3, 0.3);
                        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.5f, 1.5f);
                    }
                })
                .onTick(aoe -> {
                    for (LivingEntity entity : aoe.getEntitiesInside()) {
                        if (entity == caster) {
                            if (entity.getTicksLived() % 10 == 0) {
                                entity.getWorld().spawnParticle(Particle.ENCHANTED_HIT, entity.getLocation(), 2, 0.3, 0.3, 0.3);
                            }
                        } else if (Spellbook.canAttack(caster, entity)) {
                            if (entity.getTicksLived() % 60 == 0) { // Every 3 seconds
                                entity.addEffect(caster, Spellbook.getEffectData("Weakness"), 60, 1);
                                entity.getWorld().spawnParticle(Particle.WHITE_ASH, entity.getLocation(), 3, 0.2, 0.2, 0.2);
                            }
                        }
                    }
                });

        caster.getWorld().playSound(target.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 1.5f);
        caster.getWorld().playSound(target.getLocation(), Sound.BLOCK_BELL_RESONATE, 1.0f, 0.5f);

        double physicalDamage = Spellbook.getVariedAttributeBasedDamage(data, caster, target, true, Attribute.ADVANTAGE_PHYSICAL);
        double magicalDamage = 0;

        for (int i = 0; i < judgementStacks; i++) {
            magicalDamage += bonusDamagePerStack;
            target.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, target.getLocation().add(0, 1, 0), 3, 0.3, 0.3, 0.3);
            removeJudgement(target);
        }

        target.damage(physicalDamage, caster, PDamageType.PHYSICAL);
        target.damage(magicalDamage, caster, PDamageType.MAGIC);

        target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5);
        target.getWorld().spawnParticle(Particle.ENCHANTED_HIT, target.getLocation(), 8, 0.3, 0.3, 0.3);

        triggerTraits(1);

        if (judgementStacks > 0) {
            int furyDuration = (int) Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_MAGICAL, furyDurationMin, furyDurationMax, "furyDuration") * 20;
            caster.addEffect(caster, fury, furyDuration, judgementStacks * furyStacksPerStack);

            caster.getWorld().spawnParticle(Particle.FLAME, caster.getLocation().add(0, 1, 0), judgementStacks * 2, 0.5, 0.5, 0.5);
            caster.getWorld().spawnParticle(Particle.ENCHANTED_HIT, caster.getLocation(), judgementStacks, 0.3, 0.3, 0.3);
            caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_CONDUIT_ACTIVATE, 1.0f, 0.5f);

            triggerTraits(2);
        }

        return super.onCast();
    }

    @Override
    protected void addSpellPlaceholders() {
        spellAddedPlaceholders.add(Component.text(Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_MAGICAL, furyDurationMin, furyDurationMax, "furyDuration"), VALUE_COLOR));
        placeholderNames.add("furyDuration");
    }
}
