package de.erethon.spellbook.effects.ranger;

import de.erethon.papyrus.DamageType;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellEffect;
import org.bukkit.entity.LivingEntity;

public class DamagingRevelationEffect extends SpellEffect {

    private final int bonusDamage = data.getInt("bonusDamage", 10);


    public DamagingRevelationEffect(EffectData data, LivingEntity caster, LivingEntity target, int duration, int stacks) {
        super(data, caster, target, duration, stacks);
    }

    @Override
    public double onDamage(LivingEntity damager, double damage, DamageType type) {
        if (damager == caster) {
            return damage + bonusDamage;
        }
        return super.onDamage(damager, damage, type);
    }
}
