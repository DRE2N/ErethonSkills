package de.erethon.spellbook.traits.ranger;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellEffect;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.entity.LivingEntity;

public class LightningSpeed extends SpellTrait {

    private final EffectData effectData = Spellbook.getEffectData("Slow");

    public LightningSpeed(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected boolean onAddEffect(SpellEffect effect, boolean isNew) {
        if (effect.data == effectData) {
            return false;
        }
        return super.onAddEffect(effect, isNew);
    }
}
