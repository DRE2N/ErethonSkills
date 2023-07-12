package de.erethon.spellbook.traits.ranger;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import de.erethon.spellbook.api.TraitTrigger;
import org.bukkit.entity.LivingEntity;

public class DamagingRevelation extends SpellTrait {

    private final EffectData effect = Spellbook.getEffectData("DamagingRevelation");
    private final int effectDuration = data.getInt("effectDuration", 100);
    public DamagingRevelation(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onTrigger(TraitTrigger trigger) {
        for (LivingEntity living : trigger.getTargets()) {
            living.addEffect(caster, effect, effectDuration, 1);
        }
    }
}
