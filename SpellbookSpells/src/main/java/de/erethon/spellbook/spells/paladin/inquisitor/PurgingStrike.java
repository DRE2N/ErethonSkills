package de.erethon.spellbook.spells.paladin.inquisitor;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellEffect;
import net.kyori.adventure.text.Component;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;

import java.util.HashSet;
import java.util.Set;

public class PurgingStrike extends InquisitorBaseSpell {

    // Strike an enemy with your spear, removing a positive effect from them per stack of Judgement.
    // The effect is removed from the target and applied to the caster.
    // Additionally, the target is stunned per stack of Judgement.

    private final int range = data.getInt("range", 3);
    private final int stunDuration =  data.getInt("stunDuration", 20);
    private final int effectDurationMin = data.getInt("stunDurationMin", 1) * 20;
    private final int effectDurationMax = data.getInt("stunDurationMax", 4) * 20;

    private final EffectData stun = Spellbook.getEffectData("Stun");

    public PurgingStrike(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onPrecast() {
        return super.onPrecast() && lookForTarget(range);
    }

    @Override
    public boolean onCast() {
        Set<EffectData> effects = new HashSet<>();
        for (SpellEffect effect : target.getEffects()) {
            if (effect.data.isPositive()) {
                effects.add(effect.data);
            }
        }
        int stacks = getJudgementStacksOnTarget(target);
        if (stacks > 0) {
            for (int i = 0; i < stacks; i++) {
                if (effects.isEmpty()) break;
                EffectData effect = effects.iterator().next();
                effects.remove(effect);
                target.removeEffect(effect);
                int effectDuration = (int) Spellbook.getRangedValue(data, caster, target, Attribute.ADVANTAGE_MAGICAL, effectDurationMin, effectDurationMax, "stunDuration");
                caster.addEffect(target, effect, effectDuration, 1);
                target.getWorld().playSound(target, Sound.ENTITY_WANDERING_TRADER_DISAPPEARED, 0.7f, 1f);
                triggerTraits(target);
            }
        }
        int stunStacks = stacks * stunDuration;
        target.addEffect(caster, stun, stunStacks, 1);
        return super.onCast();
    }

    @Override
    protected void addSpellPlaceholders() {
        spellAddedPlaceholders.add(Component.text(Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_MAGICAL, effectDurationMin, effectDurationMax, "stunDuration") / 20));
        placeholderNames.add("stunDuration");
    }
}
