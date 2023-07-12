package de.erethon.spellbook.traits.ranger;

import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellEffect;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import de.erethon.spellbook.api.TraitTrigger;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;

public class ArrowExpert extends SpellTrait {

    private final EffectData bleeding = Bukkit.getServer().getSpellbookAPI().getLibrary().getEffectByID("bleeding");
    private final int bonusDuration = data.getInt("bonusDuration", 40);
    private final int bonusStacks = data.getInt("bonusStacks", 1);

    public ArrowExpert(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onTrigger(TraitTrigger trigger) {
        for (LivingEntity living : trigger.getTargets()) {
            for (SpellEffect effect : living.getEffects()) {
                if (effect.data == bleeding) {
                    effect.add(bonusDuration, bonusStacks);
                }
            }
        }
    }
}
