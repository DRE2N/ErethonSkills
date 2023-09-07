package de.erethon.spellbook.traits.assassin;

import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.SpellbookSpell;
import de.erethon.spellbook.api.TraitData;
import de.erethon.spellbook.spells.assassin.Masterthief;
import org.bukkit.entity.LivingEntity;

public class MasterMasterThief extends SpellTrait {

    private final int bonusDuration = data.getInt("bonusDuration", 500);
    public MasterMasterThief(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected SpellbookSpell onSpellCast(SpellbookSpell cast) {
        if (cast instanceof Masterthief spell) {
            spell.duration += bonusDuration;
        }
        return super.onSpellCast(cast);
    }
}
