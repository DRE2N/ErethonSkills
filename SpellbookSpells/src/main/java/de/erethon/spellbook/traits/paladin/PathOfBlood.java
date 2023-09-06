package de.erethon.spellbook.traits.paladin;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import de.erethon.spellbook.api.TraitTrigger;
import org.bukkit.entity.LivingEntity;

public class PathOfBlood extends SpellTrait {

    private final int singleDuration = data.getInt("singleTarget.duration", 120);
    private final int singleStacks = data.getInt("singleTarget.stacks", 3);
    private final int aoeDuration = data.getInt("aoe.duration", 120);
    private final int aoeStacks = data.getInt("aoe.stacks", 3);
    private final EffectData effectData = Spellbook.getEffectData("Bleeding");

    public PathOfBlood(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onTrigger(TraitTrigger trigger) {
        if (trigger.getTarget() != null) {
            trigger.getTarget().addEffect(caster, effectData, singleDuration, singleStacks);
        }
        if (trigger.getTargets() != null && !trigger.getTargets().isEmpty()) {
            for (LivingEntity target : trigger.getTargets()) {
                target.addEffect(caster, effectData, aoeDuration, aoeStacks);
            }
        }
    }
}
