package de.erethon.spellbook.traits.warrior;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellEffect;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.entity.LivingEntity;

public class Warmonger extends SpellTrait {

    private final int duration = data.getInt("duration", 400);
    private final int stacks = data.getInt("stacks", 10);
    private final EffectData effectData = Spellbook.getEffectData("Power");

    public Warmonger(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected boolean onAddEffect(SpellEffect effect, boolean isNew) {
        caster.addEffect(caster, effectData, duration, stacks);
        return super.onAddEffect(effect, isNew);
    }
}
