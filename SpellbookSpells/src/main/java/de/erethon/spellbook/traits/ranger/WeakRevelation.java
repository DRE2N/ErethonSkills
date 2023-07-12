package de.erethon.spellbook.traits.ranger;

import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import de.erethon.spellbook.api.TraitTrigger;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;

public class WeakRevelation extends SpellTrait {

    private final EffectData weakness = Bukkit.getServer().getSpellbookAPI().getLibrary().getEffectByID("Weakness");
    private final int weaknessDuration = data.getInt("weaknessDuration", 100);
    private final int weaknessStacks = data.getInt("weaknessStacks", 1);

    public WeakRevelation(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onTrigger(TraitTrigger trigger) {
        for (LivingEntity living : trigger.getTargets()) {
            living.addEffect(caster, weakness, weaknessDuration, weaknessStacks);
        }
    }
}
