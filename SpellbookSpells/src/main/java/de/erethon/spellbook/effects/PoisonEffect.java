package de.erethon.spellbook.effects;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellEffect;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;

public class PoisonEffect extends SpellEffect {

    private int tick = 0;

    public PoisonEffect(EffectData data, LivingEntity caster, LivingEntity target, int duration, int stacks) {
        super(data, caster, target, duration, stacks);
    }

    @Override
    public void onTick() {
        tick++;
        if (tick >= 20) {
            tick = 0;
            target.damage(Spellbook.getScaledValue(data, caster, target, Attribute.ADVANTAGE_MAGICAL), PDamageType.MAGIC);
        }
    }
}
