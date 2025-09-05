package de.erethon.spellbook.spells.warrior.vanguard;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.warrior.WarriorBaseSpell;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;

public class ShieldBreaker extends WarriorBaseSpell {

    // A powerful overhead chop that deals moderate damage. If the target has Stability or Resistance,
    // this ability consumes those effects and briefly Stuns them. If they have neither, it applies a moderate Slow instead.

    private final int range = data.getInt("range", 4);
    private final int stunDuration = data.getInt("stunDuration", 3) * 20;
    private final int slowDuration = data.getInt("slowDuration", 4) * 20;
    private final int slowStacks = data.getInt("slowStacks", 2);

    private final EffectData stabilityEffectData = Spellbook.getEffectData("Stability");
    private final EffectData resistanceEffectData = Spellbook.getEffectData("Resistance");
    private final EffectData stunEffectData = Spellbook.getEffectData("Stun");
    private final EffectData slowEffectData = Spellbook.getEffectData("Slow");

    public ShieldBreaker(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onPrecast() {
        return super.onPrecast() && lookForTarget(range);
    }

    @Override
    public boolean onCast() {
        playWindupEffect();

        new BukkitRunnable() {
            @Override
            public void run() {
                executeStrike();
            }
        }.runTaskLater(Spellbook.getInstance().getImplementer(), 10L);

        return super.onCast();
    }

    private void playWindupEffect() {
        caster.getWorld().spawnParticle(Particle.SWEEP_ATTACK, caster.getLocation().add(0, 1.5, 0), 1, 0, 0, 0, 0);
        caster.getWorld().playSound(caster.getLocation(), Sound.ITEM_AXE_SCRAPE, 0.8f, 0.9f);
    }

    private void executeStrike() {
        boolean hasStability = target.hasEffect(stabilityEffectData);
        boolean hasResistance = target.hasEffect(resistanceEffectData);

        double physicalDamage = Spellbook.getVariedAttributeBasedDamage(data, caster, target, true, Attribute.ADVANTAGE_PHYSICAL);
        target.damage(physicalDamage, caster, PDamageType.PHYSICAL);

        playStrikeEffects();

        if (hasStability || hasResistance) {
            if (hasStability) {
                target.removeEffect(stabilityEffectData);
            }
            if (hasResistance) {
                target.removeEffect(resistanceEffectData);
            }

            target.addEffect(caster, stunEffectData, stunDuration, 1);

            target.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, target.getLocation().add(0, 1, 0), 15, 0.5, 0.5, 0.5, 0.1);
            target.getWorld().playSound(target.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.6f, 1.2f);
        } else {
            target.addEffect(caster, slowEffectData, slowDuration, slowStacks);

            target.getWorld().spawnParticle(Particle.BLOCK_CRUMBLE, target.getLocation().add(0, 0.5, 0), 12, 0.3, 0.3, 0.3, 0.1, Material.DIRT.createBlockData());
            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 0.8f, 0.8f);
        }
    }

    private void playStrikeEffects() {
        target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 20, 0.4, 0.4, 0.4, 0.2);
        target.getWorld().spawnParticle(Particle.ENCHANTED_HIT, target.getLocation().add(0, 1, 0), 8, 0.3, 0.3, 0.3, 0.1);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 0.9f);
    }
}
