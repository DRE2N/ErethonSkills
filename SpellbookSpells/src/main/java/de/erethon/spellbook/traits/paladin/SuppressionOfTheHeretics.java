package de.erethon.spellbook.traits.paladin;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.SpellbookSpell;
import de.erethon.spellbook.api.TraitData;
import de.erethon.spellbook.api.TraitTrigger;
import de.erethon.spellbook.spells.paladin.JudgementOfGod;
import org.bukkit.entity.LivingEntity;

public class SuppressionOfTheHeretics extends SpellTrait {

    private final double durationMultiplier = data.getDouble("durationMultiplier", 2.0);
    private final int weaknessDuration = (data.getInt("weaknessDuration", 100));
    private final int weaknessStacks = data.getInt("weaknessStacks", 1);
    private final EffectData effectData = Spellbook.getEffectData("Weakness");

    public SuppressionOfTheHeretics(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected SpellbookSpell onSpellCast(SpellbookSpell cast) {
        if (cast instanceof JudgementOfGod spell) {
            spell.duration *= (int) durationMultiplier;
        }
        return super.onSpellCast(cast);
    }

    @Override
    protected void onTrigger(TraitTrigger trigger) {
        trigger.getTarget().addEffect(caster, effectData, weaknessDuration, weaknessStacks);
    }
}
