package de.erethon.spellbook.traits.paladin;

import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import de.erethon.spellbook.api.TraitTrigger;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;

public class StunningHit extends SpellTrait {

    private final int stunDuration = data.getInt("stunDuration", 40);
    private final EffectData stun = Bukkit.getServer().getSpellbookAPI().getLibrary().getEffectByID("Stun");

    public StunningHit(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onTrigger(TraitTrigger trigger) {
        trigger.getTarget().addEffect(caster, stun, stunDuration, 1);
    }
}
