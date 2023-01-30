package de.erethon.spellbook.effects.archer;

import de.erethon.papyrus.DamageType;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellEffect;
import org.bukkit.entity.LivingEntity;

public class StackingArrowDebuff extends SpellEffect {

    public StackingArrowDebuff(EffectData data, LivingEntity caster, LivingEntity target, int duration, int stacks) {
        super(data, caster, target, duration, stacks);
    }

    @Override
    public double onDamage(LivingEntity damager, double damage, DamageType type) {
        for (int i = 0; i <= stacks; i++) {
            damage = damage + data.getDouble("bonusDamage", 5.0);
            target.setFreezeTicks(target.getFreezeTicks() + 30);
        }
        return super.onDamage(damager, damage, type);
    }

    @Override
    public void onTick() {
        super.onTick();
    }

    @Override
    public void onRemove() {
        super.onRemove();
        target.setFreezeTicks(0);
    }
}
