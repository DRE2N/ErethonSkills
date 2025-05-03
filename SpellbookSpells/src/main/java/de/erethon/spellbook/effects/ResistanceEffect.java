package de.erethon.spellbook.effects;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellEffect;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.entity.LivingEntity;

public class ResistanceEffect extends SpellTrait {

    private final double damageModifier = data.getDouble("damageModifier", 0.8);

    public ResistanceEffect(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected boolean onAddEffect(SpellEffect effect, boolean isNew) {
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
