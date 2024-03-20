package de.erethon.spellbook.effects;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellEffect;
import de.erethon.spellbook.api.SpellbookSpell;
import org.bukkit.entity.LivingEntity;

public class ConfusionEffect extends SpellEffect {

    public ConfusionEffect(EffectData data, LivingEntity caster, LivingEntity target, int duration, int stacks) {
        super(data, caster, target, duration, stacks);
    }

    @Override
    public double onAttack(LivingEntity attackTarget, double damage, PDamageType type) {
        for (int i = 0; i <= stacks; i++) {
            target.damage(data.getDouble("attackDamage", 1.0), PDamageType.PHYSICAL);
        }
        return super.onAttack(target, damage, type);
    }

    @Override
    public boolean onCast(SpellbookSpell spell) {
        for (int i = 0; i <= stacks; i++) {
            target.damage(data.getDouble("castDamage", 1.0), PDamageType.MAGIC);
        }
        return true;
    }
}

