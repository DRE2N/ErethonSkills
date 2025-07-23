package de.erethon.spellbook.effects;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellEffect;
import org.bukkit.entity.LivingEntity;

public class ResistanceEffect extends SpellEffect {

    private final double damageModifier = data.getDouble("damageModifier", 0.8);

    public ResistanceEffect(EffectData data, LivingEntity caster, LivingEntity target, int duration, int stacks) {
        super(data, caster, target, duration, stacks);
    }

    @Override
    public boolean onAddEffect(SpellEffect effect, boolean isNew) {
        if (!effect.data.isPositive()) {
            return false;
        }
        return super.onAddEffect(effect, isNew);
    }

    @Override
    public double onDamage(LivingEntity attacker, double damage, PDamageType type) {
        damage *= damageModifier;
        return super.onDamage(attacker, damage, type);
    }
}
