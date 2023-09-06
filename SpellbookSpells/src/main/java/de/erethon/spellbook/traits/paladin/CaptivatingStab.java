package de.erethon.spellbook.traits.paladin;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.SpellbookSpell;
import de.erethon.spellbook.api.TraitData;
import de.erethon.spellbook.api.TraitTrigger;
import de.erethon.spellbook.spells.paladin.IceSpear;
import org.bukkit.entity.LivingEntity;

public class CaptivatingStab extends SpellTrait {

    private final EffectData data = Spellbook.getEffectData("Stun");
    private final int duration = data.getInt("duration", 20);

    public CaptivatingStab(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onTrigger(TraitTrigger trigger) {
        trigger.getTarget().addEffect(caster, data, duration, 1);
    }

    @Override
    protected SpellbookSpell onSpellCast(SpellbookSpell cast) {
        if (cast instanceof IceSpear spear) {
            spear.shouldSlow = false;
        }
        return cast;
    }
}
