package de.erethon.spellbook.traits.assassin;

import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.SpellbookSpell;
import de.erethon.spellbook.api.TraitData;
import de.erethon.spellbook.spells.assassin.DashBack;
import org.bukkit.entity.LivingEntity;

public class Flexible extends SpellTrait {

    private int cost = data.getInt("cost", 10);

    public Flexible(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected SpellbookSpell onSpellCast(SpellbookSpell cast) {
        if (cast instanceof DashBack dash) {
            dash.energyCost = cost;
        }
        return super.onSpellCast(cast);
    }
}
