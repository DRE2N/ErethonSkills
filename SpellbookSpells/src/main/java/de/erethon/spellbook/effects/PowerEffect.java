package de.erethon.spellbook.effects;

import de.erethon.papyrus.DamageType;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellEffect;
import org.bukkit.entity.LivingEntity;

public class PowerEffect extends SpellEffect {

    public PowerEffect(EffectData data, LivingEntity caster, LivingEntity target, int duration, int stacks) {
        super(data, caster, target, duration, stacks);
    }

    @Override
    public double onAttack(LivingEntity target, double damage, DamageType type) {
        for (int i = 0; i <= stacks; i++) {
            damage = damage + data.getDouble("bonusDamage", 1.0);
        }
        return super.onAttack(target, damage, type);
    }
}
