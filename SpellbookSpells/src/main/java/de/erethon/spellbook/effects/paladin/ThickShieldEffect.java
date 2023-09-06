package de.erethon.spellbook.effects.paladin;

import de.erethon.papyrus.DamageType;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellEffect;
import org.bukkit.entity.LivingEntity;

public class ThickShieldEffect extends SpellEffect {

    private final double damageMultiplier = data.getDouble("damageMultiplier", 0.5);

    public ThickShieldEffect(EffectData data, LivingEntity caster, LivingEntity target, int duration, int stacks) {
        super(data, caster, target, duration, stacks);
    }

    @Override
    public double onDamage(LivingEntity damager, double damage, DamageType type) {
        return damage * damageMultiplier;
    }
}
