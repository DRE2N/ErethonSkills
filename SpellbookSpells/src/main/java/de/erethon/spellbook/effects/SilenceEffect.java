package de.erethon.spellbook.effects;

import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellEffect;
import de.erethon.spellbook.api.SpellbookSpell;
import org.bukkit.entity.LivingEntity;

public class SilenceEffect extends SpellEffect {

    public SilenceEffect(EffectData data, LivingEntity caster, LivingEntity target, int duration, int stacks) {
        super(data, caster, target, duration, stacks);
    }

    @Override
    public boolean onCast(SpellbookSpell spell) {
        return false;
    }
}
