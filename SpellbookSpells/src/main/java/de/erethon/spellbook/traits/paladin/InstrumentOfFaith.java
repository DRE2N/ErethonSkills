package de.erethon.spellbook.traits.paladin;

import de.erethon.papyrus.DamageType;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.entity.LivingEntity;

public class InstrumentOfFaith extends SpellTrait {

    private final double bonusDamage = data.getDouble("bonusDamage", 10);

    public InstrumentOfFaith(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    public double onAttack(LivingEntity target, double damage, DamageType type) {
        return damage + bonusDamage;
    }
}
