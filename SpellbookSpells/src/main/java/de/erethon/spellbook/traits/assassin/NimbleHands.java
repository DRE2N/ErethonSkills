package de.erethon.spellbook.traits.assassin;

import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.SpellbookSpell;
import de.erethon.spellbook.api.TraitData;
import de.erethon.spellbook.spells.assassin.SwordCleave;
import org.bukkit.entity.LivingEntity;

public class NimbleHands extends SpellTrait {

    private final double damageReductionBonus = data.getDouble("damageReductionBonus", 0.3);

    public NimbleHands(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected SpellbookSpell onSpellCast(SpellbookSpell cast) {
        if (cast instanceof SwordCleave cleave) {
            cleave.damageMultiplier += damageReductionBonus;
        }
        return super.onSpellCast(cast);
    }
}
