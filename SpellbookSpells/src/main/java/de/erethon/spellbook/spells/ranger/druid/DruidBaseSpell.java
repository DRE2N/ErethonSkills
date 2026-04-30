package de.erethon.spellbook.spells.ranger.druid;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellEffect;
import de.erethon.spellbook.spells.ranger.RangerBaseSpell;
import de.erethon.spellbook.utils.SpellbookCommonMessages;
import de.slikey.effectlib.effect.LineEffect;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

public class DruidBaseSpell extends RangerBaseSpell {

    protected static final Color DRUID_PRIMARY = Color.fromRGB(92, 176, 76);
    protected static final Color DRUID_SECONDARY = Color.fromRGB(178, 214, 95);
    protected static final Color DRUID_GOLD = Color.fromRGB(234, 207, 96);

    protected final int seedDuration = data.getInt("seedDuration", 140);
    protected final int maxSeedStacks = data.getInt("maxSeedStacks", 3);
    protected final EffectData seed = Spellbook.getEffectData("DruidSeed");
    protected final EffectData slow = Spellbook.getEffectData("Slow");
    protected final EffectData regeneration = Spellbook.getEffectData("Regeneration");
    protected final EffectData resistance = Spellbook.getEffectData("Resistance");
    protected final EffectData stability = Spellbook.getEffectData("Stability");

    public DruidBaseSpell(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    protected boolean lookForAnyLivingTarget(double range) {
        Entity entity = caster.getTargetEntity((int) range);
        if (!(entity instanceof LivingEntity living)) {
            caster.sendParsedActionBar(SpellbookCommonMessages.NO_TARGET);
            return false;
        }
        target = living;
        return true;
    }

    protected Location getTargetGround(int range) {
        Block block = caster.getTargetBlockExact(range);
        if (block == null || !block.isSolid()) {
            caster.sendParsedActionBar(SpellbookCommonMessages.NO_TARGET);
            return null;
        }
        return block.getLocation().add(0.5, 1, 0.5);
    }

    protected boolean isEnemy(LivingEntity entity) {
        return Spellbook.canAttack(caster, entity);
    }

    protected boolean isAlly(LivingEntity entity) {
        return entity.equals(caster) || !Spellbook.canAttack(caster, entity);
    }

    protected int getSeedStacks(LivingEntity entity) {
        if (seed == null) {
            return 0;
        }
        for (SpellEffect effect : entity.getEffects()) {
            if (effect.data == seed) {
                return effect.getStacks();
            }
        }
        return 0;
    }

    protected void addSeed(LivingEntity entity, int stacks) {
        if (seed == null || stacks <= 0) {
            return;
        }
        int currentStacks = getSeedStacks(entity);
        if (currentStacks >= maxSeedStacks) {
            playSeedEffect(entity, currentStacks);
            return;
        }
        int stacksToAdd = Math.min(stacks, Math.max(1, maxSeedStacks - currentStacks));
        entity.addEffect(caster, seed, seedDuration, stacksToAdd);
        playSeedEffect(entity, Math.min(maxSeedStacks, currentStacks + stacksToAdd));
    }

    protected int consumeSeeds(LivingEntity entity) {
        int stacks = getSeedStacks(entity);
        if (stacks > 0 && seed != null) {
            entity.removeEffect(seed);
            playSeedConsumeEffect(entity, stacks);
        }
        return stacks;
    }

    protected void applySlow(LivingEntity entity, int duration, int stacks) {
        if (slow != null && duration > 0) {
            entity.addEffect(caster, slow, duration, stacks);
        }
    }

    protected void applyRegeneration(LivingEntity entity, int duration, int stacks) {
        if (regeneration != null && duration > 0) {
            entity.addEffect(caster, regeneration, duration, stacks);
        }
    }

    protected void playSeedEffect(LivingEntity entity, int stacks) {
        Location loc = entity.getLocation().add(0, 1.1, 0);
        entity.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc, 2 + stacks, 0.25, 0.35, 0.25, 0.01);
        entity.getWorld().spawnParticle(Particle.DUST, loc, 4 + stacks, 0.25, 0.35, 0.25, 0,
            new Particle.DustOptions(DRUID_PRIMARY, 0.9f));
    }

    protected void playSeedConsumeEffect(LivingEntity entity, int stacks) {
        Location loc = entity.getLocation().add(0, 1.2, 0);
        entity.getWorld().spawnParticle(Particle.SPORE_BLOSSOM_AIR, loc, 8 + stacks * 3, 0.35, 0.45, 0.35, 0.02);
        entity.getWorld().spawnParticle(Particle.DUST, loc, 5 + stacks * 2, 0.3, 0.4, 0.3, 0,
            new Particle.DustOptions(DRUID_GOLD, 1.0f));
        entity.getWorld().playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_BREAK, SoundCategory.RECORDS, 0.45f, 1.4f);
    }

    protected void playVineLine(Location from, Location to, int ticks) {
        LineEffect line = new LineEffect(Spellbook.getInstance().getEffectManager());
        line.setLocation(from);
        line.setTargetLocation(to);
        line.particle = Particle.DUST;
        line.particleSize = 0.45f;
        line.color = DRUID_PRIMARY;
        line.iterations = ticks;
        line.start();
    }

    protected void pullEntity(LivingEntity entity, Vector direction, double strength) {
        entity.setFallDistance(0);
        entity.setVelocity(direction.clone().normalize().multiply(strength).setY(Math.max(direction.getY() * strength, 0.15)));
    }
}
