package de.erethon.spellbook.traits.assassin;

import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.SpellbookSpell;
import de.erethon.spellbook.api.TraitData;
import de.erethon.spellbook.spells.assassin.SwordStorm;
import org.bukkit.entity.LivingEntity;

public class LightBlade extends SpellTrait {

    private final int bonusAttacks = data.getInt("bonusAttacks", 5);

    public LightBlade(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected SpellbookSpell onSpellCast(SpellbookSpell cast) {
        if (cast instanceof SwordStorm storm) {
            storm.attacks += bonusAttacks;
        }
        return super.onSpellCast(cast);
    }
}
