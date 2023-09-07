package de.erethon.spellbook.traits.assassin;

import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.SpellbookSpell;
import de.erethon.spellbook.api.TraitData;
import de.erethon.spellbook.spells.assassin.AssassinBaseTrap;
import org.bukkit.entity.LivingEntity;

public class BearTrap extends SpellTrait {

    private final double damageMultiplier = data.getDouble("damageMultiplier", 1.2);
    public BearTrap(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected SpellbookSpell onSpellCast(SpellbookSpell cast) {
        if (cast instanceof AssassinBaseTrap trap) {
            trap.damageMultiplier = damageMultiplier;
        }
        return super.onSpellCast(cast);
    }
}
