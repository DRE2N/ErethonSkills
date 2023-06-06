package de.erethon.spellbook.effects;

import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellEffect;
import de.erethon.spellbook.api.SpellTrait;
import org.bukkit.entity.LivingEntity;

import java.util.HashSet;
import java.util.Set;

public class BreakEffect extends SpellEffect {

    private final Set<SpellTrait> traits = new HashSet<>();

    public BreakEffect(EffectData data, LivingEntity caster, LivingEntity target, int duration, int stacks) {
        super(data, caster, target, duration, stacks);
    }

    @Override
    public void onApply() {
        for (SpellTrait trait : target.getActiveTraits()) {
            if (trait.isActive()) {
                trait.setActive(false);
                traits.add(trait); // We only want to re-enable traits that were active before the effect was applied
            }
        }
    }

    @Override
    public void onRemove() {
        for (SpellTrait trait : traits) {
            trait.setActive(true);
        }
    }
}
