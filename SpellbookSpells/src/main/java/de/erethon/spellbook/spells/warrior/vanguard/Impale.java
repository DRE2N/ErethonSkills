package de.erethon.spellbook.spells.warrior.vanguard;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.warrior.WarriorBaseSpell;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;

public class Impale extends VanguardBaseSpell {

    // A powerful forward thrust with your axe. Deals high damage and applies Bleed.
    // If the target is below 40% health, the Bleed effect's duration is doubled.

    private final int range = data.getInt("range", 4);
    private final double healthThreshold = data.getDouble("healthThreshold", 0.4);
    private final int minBleedDuration = data.getInt("minBleedDuration", 4) * 20;
    private final int maxBleedDuration = data.getInt("maxBleedDuration", 8) * 20;
    private final int minBleedStacks = data.getInt("minBleedStacks", 2);
    private final int maxBleedStacks = data.getInt("maxBleedStacks", 4);
    private final int windupTicks = data.getInt("windupTicks", 8);

    private final EffectData bleedEffectData = Spellbook.getEffectData("Bleeding");

    private int ticksElapsed = 0;
    private boolean hasStruck = false;

    public Impale(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = windupTicks + 5;
    }

    @Override
    protected boolean onPrecast() {
        return super.onPrecast() && lookForTarget(range);
    }

    @Override
    public boolean onCast() {
        playWindupEffect();
        return super.onCast();
    }

    @Override
    protected void onTick() {
        ticksElapsed++;

        if (ticksElapsed == windupTicks && !hasStruck) {
            executeThrust();
            hasStruck = true;
        }
    }

    private void playWindupEffect() {
        caster.getWorld().spawnParticle(Particle.CRIT, caster.getLocation().add(0, 1.5, 0), 8, 0.5, 0.3, 0.5, 0.1);
        caster.getWorld().playSound(caster.getLocation(), Sound.ITEM_CROSSBOW_LOADING_START, 0.8f, 1.2f);
    }

    private void executeThrust() {
        double physicalDamage = Spellbook.getVariedAttributeBasedDamage(data, caster, target, true, Attribute.ADVANTAGE_PHYSICAL);
        target.damage(physicalDamage, caster, PDamageType.PHYSICAL);

        double currentHealthPercent = target.getHealth() / target.getAttribute(Attribute.MAX_HEALTH).getValue();
        boolean isLowHealth = currentHealthPercent <= healthThreshold;

        int bleedDuration = (int) Spellbook.getRangedValue(data, caster, target, Attribute.ADVANTAGE_PHYSICAL, minBleedDuration, maxBleedDuration, "bleedDuration");
        int bleedStacks = (int) Spellbook.getRangedValue(data, caster, target, Attribute.ADVANTAGE_PHYSICAL, minBleedStacks, maxBleedStacks, "bleedStacks");

        if (isLowHealth) {
            bleedDuration *= 2;
        }

        target.addEffect(caster, bleedEffectData, bleedDuration, bleedStacks);

        playThrustEffects(isLowHealth);
    }

    private void playThrustEffects(boolean isLowHealth) {
        target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 25, 0.4, 0.4, 0.4, 0.3);
        target.getWorld().spawnParticle(Particle.ENCHANTED_HIT, target.getLocation().add(0, 1, 0), 12, 0.3, 0.3, 0.3, 0.2);

        if (isLowHealth) {
            target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0,
                new Particle.DustOptions(org.bukkit.Color.MAROON, 2.0f));
            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 0.7f);
            target.getWorld().playSound(target.getLocation(), Sound.BLOCK_CONDUIT_ATTACK_TARGET, 0.8f, 1.5f);
        } else {
            target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(0, 1, 0), 12, 0.3, 0.3, 0.3, 0,
                new Particle.DustOptions(org.bukkit.Color.RED, 1.5f));
            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.0f);
        }

        caster.getWorld().playSound(caster.getLocation(), Sound.ITEM_CROSSBOW_SHOOT, 1.0f, 1.3f);
    }
}
