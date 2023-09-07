package de.erethon.spellbook.traits.assassin;

import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.SpellbookSpell;
import de.erethon.spellbook.api.TraitData;
import de.erethon.spellbook.spells.assassin.AssassinBaseTrap;
import org.bukkit.entity.LivingEntity;

public class BombSpecialist extends SpellTrait {

    private final int radiusBonus = data.getInt("bonusRadius", 2);

    public BombSpecialist(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected SpellbookSpell onSpellCast(SpellbookSpell cast) {
        if (cast instanceof AssassinBaseTrap trap) {
            trap.size += radiusBonus;
        }
        return super.onSpellCast(cast);
    }
}
