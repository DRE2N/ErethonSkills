package de.erethon.spellbook.effects;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellEffect;
import org.bukkit.entity.LivingEntity;

public class WeaknessEffect extends SpellEffect {

    public WeaknessEffect(EffectData data, LivingEntity caster, LivingEntity target, int duration, int stacks) {
        super(data, caster, target, duration, stacks);
    }

    @Override
    public double onAttack(LivingEntity target, double damage, PDamageType type) {
        for (int i = 0; i <= stacks; i++) {
            damage = damage - data.getDouble("malusDamage", 1.0);
        }
        return super.onAttack(target, damage, type);
    }
}
