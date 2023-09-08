package de.erethon.spellbook.traits.ranger;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;

import de.erethon.spellbook.api.TraitTrigger;
import org.bukkit.entity.LivingEntity;


public class BurningExplosion extends SpellTrait  {

    private final int duration = data.getInt("duration", 100);
    private final int stacks = data.getInt("stacks", 2);
    private final EffectData effectData = Spellbook.getEffectData("Burning");

    public BurningExplosion(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onTrigger(TraitTrigger trigger) {
        for (LivingEntity livingEntity : trigger.getTargets()) {
            livingEntity.addEffect(caster, effectData, duration, stacks);
        }
    }
}
