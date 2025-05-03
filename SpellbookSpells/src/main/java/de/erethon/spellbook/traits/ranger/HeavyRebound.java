package de.erethon.spellbook.traits.ranger;

import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.SpellbookSpell;
import de.erethon.spellbook.api.TraitData;
import de.erethon.spellbook.spells.ranger.hawkeye.RicochetArrow;
import org.bukkit.entity.LivingEntity;

public class HeavyRebound extends SpellTrait {

    private final double damageReduction = data.getDouble("damageReductionBonus", -5);
    private final int bonusMaxRicochets = data.getInt("bonusMaxRicochets", 2);

    public HeavyRebound(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected SpellbookSpell onSpellCast(SpellbookSpell cast) {
        if (cast instanceof RicochetArrow ricochet) {
            ricochet.damageReductionPerRicochet += damageReduction;
            ricochet.maxRicochets += bonusMaxRicochets;
        }
        return super.onSpellCast(cast);
    }
}
