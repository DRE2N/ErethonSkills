package de.erethon.spellbook.traits.assassin;

import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import de.erethon.spellbook.api.TraitTrigger;
import de.erethon.spellbook.spells.assassin.TrapIron;
import org.bukkit.entity.LivingEntity;

public class DemolitionExpert extends SpellTrait {

    private final float power = (float) data.getDouble("power", 1.0f);

    public DemolitionExpert(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onTrigger(TraitTrigger trigger) {
        if (trigger.getSpell() instanceof TrapIron trap && !trap.triggeredFirstTime) {
            trap.target.createExplosion(power, false, false);
        }
    }
}
