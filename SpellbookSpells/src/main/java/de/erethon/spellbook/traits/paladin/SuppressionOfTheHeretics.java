package de.erethon.spellbook.traits.paladin;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.SpellbookSpell;
import de.erethon.spellbook.api.TraitData;
import de.erethon.spellbook.api.TraitTrigger;
import de.erethon.spellbook.spells.paladin.inquisitor.JudgementOfGod;
import org.bukkit.entity.LivingEntity;

public class SuppressionOfTheHeretics extends SpellTrait {

    private final double rangeMultiplier = data.getDouble("rangeMultiplier", 2.0);
    private final double bonusDamageMultiplier = data.getDouble("bonusDamageMultiplier", 0.3);
    private final int effectDuration = (data.getInt("effectDuration", 100));
    private final int effectStacks = data.getInt("effectStacks", 1);
    private final EffectData effectData = Spellbook.getEffectData("Bleeding");

    public SuppressionOfTheHeretics(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected SpellbookSpell onSpellCast(SpellbookSpell cast) {
        if (cast instanceof JudgementOfGod spell) {
            spell.deathDamageRange *= (int) rangeMultiplier;
            spell.deathDamageMultiplier += bonusDamageMultiplier;
        }
        return super.onSpellCast(cast);
    }

    @Override
    protected void onTrigger(TraitTrigger trigger) {
        trigger.getTarget().addEffect(caster, effectData, effectDuration, effectStacks);
    }
}
