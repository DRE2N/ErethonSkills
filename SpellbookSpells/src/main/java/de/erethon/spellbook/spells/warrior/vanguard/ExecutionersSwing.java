package de.erethon.spellbook.spells.warrior.vanguard;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.warrior.WarriorBaseSpell;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;

public class ExecutionersSwing extends WarriorBaseSpell {

    // Channel for 1.5 seconds, then unleash a single, devastating overhead blow on a target.
    // Deals massive physical damage, which is significantly increased based on the target's missing health.
    // Consumes and is empowered by any active Momentum stacks.

    private final int range = data.getInt("range", 4);
    private final int channelDuration = data.getInt("channelDuration", 3) * 20;
    private final double missingHealthMultiplier = data.getDouble("missingHealthMultiplier", 2.0);
    private final double momentumDamagePerStack = data.getDouble("momentumDamagePerStack", 0.15);
    private final int momentumPerStack = data.getInt("momentumPerStack", 10);

    private int ticksElapsed = 0;
    private boolean hasExecuted = false;
    private int momentumConsumed = 0;

    public ExecutionersSwing(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = channelDuration + 10;
    }

    @Override
    protected boolean onPrecast() {
        return super.onPrecast() && lookForTarget(range);
    }

    @Override
    public boolean onCast() {
        momentumConsumed = caster.getEnergy() / momentumPerStack;
        caster.setEnergy(0);

        playChannelStartEffect();
        return super.onCast();
    }

    @Override
    protected void onTick() {
        ticksElapsed++;

        if (ticksElapsed <= channelDuration) {
            playChannelEffect();
        } else if (ticksElapsed == channelDuration + 1 && !hasExecuted) {
            executeSwing();
            hasExecuted = true;
        }
    }

    private void playChannelStartEffect() {
        caster.getWorld().spawnParticle(Particle.ENCHANTED_HIT, caster.getLocation().add(0, 2, 0), 15, 0.5, 0.3, 0.5, 0.1);
        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.6f, 0.8f);
        caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_ANVIL_USE, 0.8f, 0.6f);
    }

    private void playChannelEffect() {
        if (ticksElapsed % 5 == 0) {
            double progress = (double) ticksElapsed / channelDuration;
            int particleCount = (int) (5 + progress * 20);

            caster.getWorld().spawnParticle(Particle.CRIT, caster.getLocation().add(0, 1.5, 0), particleCount, 0.3, 0.5, 0.3, 0.2);
            caster.getWorld().spawnParticle(Particle.DUST, caster.getLocation().add(0, 1.5, 0), (int)(particleCount * 0.5), 0.4, 0.3, 0.4, 0,
                new Particle.DustOptions(org.bukkit.Color.fromRGB(139, 0, 0), (float)(1.0 + progress)));

            float pitch = (float) (0.8f + progress * 0.6f);
            caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_GRINDSTONE_USE, 0.3f, pitch);
        }
    }

    private void executeSwing() {
        double baseDamage = Spellbook.getVariedAttributeBasedDamage(data, caster, target, true, Attribute.ADVANTAGE_PHYSICAL);

        double missingHealthPercent = 1.0 - (target.getHealth() / target.getAttribute(Attribute.MAX_HEALTH).getValue());
        double missingHealthBonus = baseDamage * missingHealthMultiplier * missingHealthPercent;

        double momentumBonus = baseDamage * momentumDamagePerStack * momentumConsumed;

        double totalDamage = baseDamage + missingHealthBonus + momentumBonus;

        target.damage(totalDamage, caster, PDamageType.PHYSICAL);

        playExecutionEffects(missingHealthPercent > 0.5, momentumConsumed > 5);
    }

    private void playExecutionEffects(boolean isHighMissingHealth, boolean hasHighMomentum) {
        target.getWorld().spawnParticle(Particle.EXPLOSION, target.getLocation().add(0, 1, 0), 1, 0, 0, 0, 0);
        target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 40, 0.6, 0.6, 0.6, 0.4);
        target.getWorld().spawnParticle(Particle.ENCHANTED_HIT, target.getLocation().add(0, 1, 0), 25, 0.5, 0.5, 0.5, 0.3);

        if (isHighMissingHealth) {
            target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(0, 1, 0), 30, 0.7, 0.7, 0.7, 0,
                new Particle.DustOptions(org.bukkit.Color.fromRGB(139, 0, 0), 2.5f));
            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_WITHER_DEATH, 0.5f, 1.2f);
        }

        if (hasHighMomentum) {
            caster.getWorld().spawnParticle(Particle.FIREWORK, caster.getLocation().add(0, 1, 0), 15, 0.5, 0.5, 0.5, 0.1);
            caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.4f, 1.5f);
        }

        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 0.6f);
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 0.8f);
        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 0.6f, 0.7f);
    }
}
