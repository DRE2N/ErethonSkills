package de.erethon.spellbook.effects.warrior;

import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellEffect;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;

/**
 * Razor Mark Effect - Applied by Bladeweaver abilities.
 * Stacks up to 5 times. Can be consumed by certain abilities for bonus effects.
 * Each stack represents a vulnerability created by the Bladeweaver's spectral blades.
 */
public class RazorMarkEffect extends SpellEffect {

    private static final Color RAZOR_MARK_COLOR = Color.fromRGB(255, 215, 0); // Gold
    private int tick = 0;

    public RazorMarkEffect(EffectData data, LivingEntity caster, LivingEntity target, int duration, int stacks) {
        super(data, caster, target, duration, stacks);
    }

    @Override
    public void onApply() {
        // Visual feedback when mark is applied
        playApplyEffect();
    }

    @Override
    public void onTick() {
        tick++;
        // Periodic visual indicator every second
        if (tick >= 20) {
            tick = 0;
            playTickEffect();
        }
    }

    @Override
    public void onRemove() {
        // Visual feedback when mark expires naturally
        playExpireEffect();
    }

    private void playApplyEffect() {
        Location loc = target.getLocation().add(0, 1.5, 0);
        Particle.DustOptions dust = new Particle.DustOptions(RAZOR_MARK_COLOR, 1.2f);
        target.getWorld().spawnParticle(Particle.DUST, loc, 8, 0.3, 0.3, 0.3, 0, dust);
        target.getWorld().playSound(loc, Sound.BLOCK_CHAIN_HIT, 0.5f, 1.8f);
    }

    private void playTickEffect() {
        Location loc = target.getLocation().add(0, 2.2, 0);
        Particle.DustOptions dust = new Particle.DustOptions(RAZOR_MARK_COLOR, 0.8f);

        // Show small particles indicating stacks
        int stacks = getStacks();
        for (int i = 0; i < stacks; i++) {
            double angle = (Math.PI * 2 * i / stacks);
            double x = Math.cos(angle) * 0.3;
            double z = Math.sin(angle) * 0.3;
            target.getWorld().spawnParticle(Particle.DUST, loc.clone().add(x, 0, z), 1, 0, 0, 0, 0, dust);
        }
    }

    private void playExpireEffect() {
        Location loc = target.getLocation().add(0, 1.5, 0);
        Particle.DustOptions dust = new Particle.DustOptions(RAZOR_MARK_COLOR, 0.6f);
        target.getWorld().spawnParticle(Particle.DUST, loc, 5, 0.2, 0.2, 0.2, 0, dust);
    }
}

