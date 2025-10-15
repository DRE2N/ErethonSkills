package de.erethon.spellbook.effects.paladin;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellEffect;
import org.bukkit.entity.LivingEntity;

public class Judgement extends SpellEffect {

    private final double magicalDamageMultiplier = data.getDouble("magicalDamageMultiplier", 1.2);

    public Judgement(EffectData data, LivingEntity caster, LivingEntity target, int duration, int stacks) {
        super(data, caster, target, duration, stacks);
    }

    @Override
    public double onDamage(LivingEntity damager, double damage, PDamageType type) {
        if (type == PDamageType.MAGIC) {
            damage *= magicalDamageMultiplier;
        }
        return super.onDamage(damager, damage, type);
    }
}
