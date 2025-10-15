package de.erethon.spellbook.spells.warrior.vanguard;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.warrior.WarriorBaseSpell;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;

public class UnflinchingStance extends VanguardBaseSpell {

    // For a few seconds, gain a significant amount of Resistance and Stability.
    // Your Momentum stacks will not be consumed when using an ability during this time.

    private final int minDuration = data.getInt("minDuration", 4) * 20;
    private final int maxDuration = data.getInt("maxDuration", 8) * 20;
    private final int minResistanceStacks = data.getInt("minResistanceStacks", 2);
    private final int maxResistanceStacks = data.getInt("maxResistanceStacks", 4);
    private final int minStabilityStacks = data.getInt("minStabilityStacks", 1);
    private final int maxStabilityStacks = data.getInt("maxStabilityStacks", 2);

    private final EffectData resistanceEffectData = Spellbook.getEffectData("Resistance");
    private final EffectData stabilityEffectData = Spellbook.getEffectData("Stability");

    public UnflinchingStance(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = (int) Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_PHYSICAL, minDuration, maxDuration);
    }

    @Override
    public boolean onCast() {
        int duration = (int) Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_PHYSICAL, minDuration, maxDuration, "duration");
        int resistanceStacks = (int) Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_PHYSICAL, minResistanceStacks, maxResistanceStacks, "resistanceStacks");
        int stabilityStacks = (int) Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_PHYSICAL, minStabilityStacks, maxStabilityStacks, "stabilityStacks");

        caster.addEffect(caster, resistanceEffectData, duration, resistanceStacks);
        caster.addEffect(caster, stabilityEffectData, duration, stabilityStacks);

        playStanceEffects();

        caster.getTags().add("warrior.unflinching_stance");

        return super.onCast();
    }

    private void playStanceEffects() {
        caster.getWorld().spawnParticle(Particle.ENCHANTED_HIT, caster.getLocation().add(0, 1, 0), 30, 0.8, 1.0, 0.8, 0.1);
        caster.getWorld().spawnParticle(Particle.DUST, caster.getLocation().add(0, 1, 0), 25, 0.6, 0.8, 0.6, 0,
            new Particle.DustOptions(org.bukkit.Color.SILVER, 1.5f));

        caster.getWorld().spawnParticle(Particle.FIREWORK, caster.getLocation().add(0, 0.5, 0), 8, 0.4, 0.2, 0.4, 0.05);

        caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_ANVIL_USE, 0.8f, 1.2f);
        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_IRON_GOLEM_REPAIR, 0.6f, 1.4f);
        caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.4f, 1.8f);
    }

    @Override
    protected void onTickFinish() {
        super.onTickFinish();
        caster.getTags().remove("warrior.unflinching_stance");
    }
}
