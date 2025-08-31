package de.erethon.spellbook.effects;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellEffect;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;

public class RegenerationEffect extends SpellEffect {

    private int tick = 0;

    public RegenerationEffect(EffectData data, LivingEntity caster, LivingEntity target, int duration, int stacks) {
        super(data, caster, target, duration, stacks);
    }

    @Override
    public void onTick() {
        tick++;
        if (tick >= 20) {
            tick = 0;
            target.heal(Spellbook.getScaledValue(data, caster, target, Attribute.STAT_HEALINGPOWER));
        }
    }
}
