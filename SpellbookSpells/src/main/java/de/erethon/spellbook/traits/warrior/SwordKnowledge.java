package de.erethon.spellbook.traits.warrior;

import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.SpellbookSpell;
import de.erethon.spellbook.api.TraitData;
import de.erethon.spellbook.spells.warrior.SlashingHit;
import org.bukkit.entity.LivingEntity;

public class SwordKnowledge extends SpellTrait {

    private final double bleedingStackMultiplier = data.getDouble("bleedingStackMultiplier", 4.0);
    private final double damageMultiplier = data.getDouble("damageMultiplier", 2.0);

    public SwordKnowledge(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected SpellbookSpell onSpellCast(SpellbookSpell cast) {
        if (cast instanceof SlashingHit spell) {
            spell.bleedingStackMultiplier *= bleedingStackMultiplier;
            spell.damageMultiplier *= damageMultiplier;
        }
        return super.onSpellCast(cast);
    }
}
