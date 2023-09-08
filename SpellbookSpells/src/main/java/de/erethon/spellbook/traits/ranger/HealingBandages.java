package de.erethon.spellbook.traits.ranger;

import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.SpellbookSpell;
import de.erethon.spellbook.api.TraitData;
import de.erethon.spellbook.spells.ranger.BasicSelfHeal;
import org.bukkit.entity.LivingEntity;

public class HealingBandages extends SpellTrait {

    private final double healingMultiplier = data.getDouble("healingMultiplier", 1.5);

    public HealingBandages(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected SpellbookSpell onSpellCast(SpellbookSpell cast) {
        if (cast instanceof BasicSelfHeal heal) {
            heal.healingMultiplier = healingMultiplier;
        }
        return super.onSpellCast(cast);
    }
}
