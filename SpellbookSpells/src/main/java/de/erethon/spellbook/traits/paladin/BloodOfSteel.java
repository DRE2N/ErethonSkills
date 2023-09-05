package de.erethon.spellbook.traits.paladin;

import de.erethon.papyrus.DamageType;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.entity.LivingEntity;

public class BloodOfSteel extends SpellTrait {

    private final double reduction = data.getDouble("reduction", 0.05);

    public BloodOfSteel(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    public double onDamage(LivingEntity attacker, double damage, DamageType type) {
        return damage * (1 - reduction);
    }
}
