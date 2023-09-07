package de.erethon.spellbook.traits.assassin;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import de.erethon.spellbook.api.TraitTrigger;
import org.bukkit.entity.LivingEntity;

public class BrutalHits extends SpellTrait {

    private final int duration = data.getInt("duration", 120);
    private final int stacks = data.getInt("stacks", 1);
    private final EffectData effectData = Spellbook.getEffectData("Bleeding");

    public BrutalHits(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onTrigger(TraitTrigger trigger) {
        if (trigger.getTarget() != null) {
            trigger.getTarget().addEffect(caster, effectData, duration, stacks);
        }
        if (trigger.getTargets() != null) {
            for (LivingEntity entity : trigger.getTargets()) {
                entity.addEffect(caster, effectData, duration, stacks);
            }
        }
    }
}
