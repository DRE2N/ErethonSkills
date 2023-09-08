package de.erethon.spellbook.traits.warrior;

import de.erethon.papyrus.DamageType;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.entity.LivingEntity;

public class Bloodthirsty extends SpellTrait {

    private final double lifestealPercentage = data.getDouble("lifestealPercentage", 0.1);

    public Bloodthirsty(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    public double onAttack(LivingEntity target, double damage, DamageType type) {
        double heal = damage * lifestealPercentage;
        caster.setHealth(Math.min(caster.getHealth() + heal, caster.getMaxHealth()));
        return super.onAttack(target, damage, type);
    }
}
