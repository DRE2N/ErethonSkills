package de.erethon.spellbook.traits.warrior;

import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.SpellbookSpell;
import de.erethon.spellbook.api.TraitData;
import de.erethon.spellbook.spells.warrior.BreakingHit;
import org.bukkit.entity.LivingEntity;

public class Destruction extends SpellTrait {

    private final double durationMultiplier = data.getDouble("durationMultiplier", 2.0);

    public Destruction(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected SpellbookSpell onSpellCast(SpellbookSpell cast) {
        if (cast instanceof BreakingHit hit) {
            hit.durationMultiplier = durationMultiplier;
        }
        return super.onSpellCast(cast);
    }
}
