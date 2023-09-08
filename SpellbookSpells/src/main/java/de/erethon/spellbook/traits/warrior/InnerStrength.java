package de.erethon.spellbook.traits.warrior;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellEffect;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.entity.LivingEntity;

public class InnerStrength extends SpellTrait {

    private final int duration = data.getInt("duration", 100);
    private final EffectData effectData = Spellbook.getEffectData("Resistance");

    public InnerStrength(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected boolean onAddEffect(SpellEffect effect, boolean isNew) {
        if (!effect.data.isPositive()) {
            caster.addEffect(caster, effectData, duration, 1);
        }
        return super.onAddEffect(effect, isNew);
    }
}
