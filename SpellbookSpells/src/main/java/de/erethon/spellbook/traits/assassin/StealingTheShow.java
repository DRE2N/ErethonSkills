package de.erethon.spellbook.traits.assassin;

import de.erethon.spellbook.api.SpellEffect;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import de.erethon.spellbook.api.TraitTrigger;
import org.bukkit.entity.LivingEntity;

public class StealingTheShow extends SpellTrait {
    public StealingTheShow(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onTrigger(TraitTrigger trigger) {
        for (SpellEffect effect : trigger.getTarget().getEffects()) {
            if (effect.data.isPositive()) {
                effect.setTarget(caster);
            }
        }
    }
}
