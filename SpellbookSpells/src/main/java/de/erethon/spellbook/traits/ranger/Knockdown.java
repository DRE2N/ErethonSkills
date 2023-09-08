package de.erethon.spellbook.traits.ranger;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import de.erethon.spellbook.api.TraitTrigger;
import org.bukkit.entity.LivingEntity;

public class Knockdown extends SpellTrait {

    private final int duration = data.getInt("duration", 20);
    private final EffectData effectData = Spellbook.getEffectData("Stun");

    public Knockdown(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onTrigger(TraitTrigger trigger) {
        trigger.getTarget().addEffect(caster, effectData, duration, 1);
    }
}
