package de.erethon.spellbook.effects;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellEffect;
import org.bukkit.entity.LivingEntity;

public class WeaknessEffect extends SpellEffect {

    private final double outgoingDamageMultiplier = data.getDouble("outgoingDamageMultiplier", 0.95);
    private final double incomingDamageMultiplier = data.getDouble("incomingDamageMultiplier", 1.05);

    public WeaknessEffect(EffectData data, LivingEntity caster, LivingEntity target, int duration, int stacks) {
        super(data, caster, target, duration, stacks);
    }

    @Override
    public double onAttack(LivingEntity target, double damage, PDamageType type) {
        for (int i = 0; i <= stacks; i++) {
            damage *= outgoingDamageMultiplier;
        }
        return super.onAttack(target, damage, type);
    }

    @Override
    public double onDamage(LivingEntity damager, double damage, PDamageType type) {
        for (int i = 0; i <= stacks; i++) {
            damage *= incomingDamageMultiplier;
        }
        return super.onDamage(damager, damage, type);
    }
}
