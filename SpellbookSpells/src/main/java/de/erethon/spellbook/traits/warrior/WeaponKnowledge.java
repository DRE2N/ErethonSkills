package de.erethon.spellbook.traits.warrior;

import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.SpellbookSpell;
import de.erethon.spellbook.api.TraitData;
import de.erethon.spellbook.spells.warrior.PreciseHit;
import org.bukkit.entity.LivingEntity;

public class WeaponKnowledge extends SpellTrait {

    private final int bonusDamage = data.getInt("bonusBonusDamage", 25);

    public WeaponKnowledge(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected SpellbookSpell onSpellCast(SpellbookSpell cast) {
        if (cast instanceof PreciseHit hit) {
            hit.BonusDamage += bonusDamage;
        }
        return super.onSpellCast(cast);
    }
}
