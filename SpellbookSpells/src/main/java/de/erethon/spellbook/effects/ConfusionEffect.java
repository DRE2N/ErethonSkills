package de.erethon.spellbook.effects;

import de.erethon.papyrus.DamageType;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellEffect;
import org.bukkit.entity.LivingEntity;

public class ConfusionEffect extends SpellEffect {

    public ConfusionEffect(EffectData data, LivingEntity caster, LivingEntity target, int duration, int stacks) {
        super(data, caster, target, duration, stacks);
    }

    @Override
    public double onAttack(SpellCaster attackTarget, double damage, DamageType type) {
        for (int i = 0; i <= stacks; i++) {
            target.damage(data.getDouble("attackDamage", 1.0), DamageType.PHYSICAL);
        }
        return super.onAttack(target, damage, type);
    }

    @Override
    public void onCast() {
        for (int i = 0; i <= stacks; i++) {
            target.damage(data.getDouble("castDamage", 1.0), DamageType.MAGIC);
        }
        super.onCast();
    }
}

