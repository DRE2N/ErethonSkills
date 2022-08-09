package de.erethon.spellbook.effects;

import de.erethon.papyrus.DamageType;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellEffect;
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
            target.damage(data.getDouble("damage", 1.0), DamageType.MAGIC);
        }
    }
}