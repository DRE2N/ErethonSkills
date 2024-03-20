package de.erethon.spellbook.traits.warrior;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.entity.LivingEntity;

public class Champion extends SpellTrait {

    private final double damageMultiplier = data.getDouble("damageMultiplier", 1.5);

    public Champion(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    public double onAttack(LivingEntity target, double damage, PDamageType type) {
        if (caster.getEnergy() == caster.getMaxEnergy()) {
            return damage * damageMultiplier;
        }
        return super.onAttack(target, damage, type);
    }
}
