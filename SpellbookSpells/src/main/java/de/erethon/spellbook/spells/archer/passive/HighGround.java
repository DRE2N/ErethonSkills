package de.erethon.spellbook.spells.archer.passive;

import de.erethon.papyrus.DamageType;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.PassiveSpell;
import org.bukkit.entity.LivingEntity;

public class HighGround extends PassiveSpell {

    private final double damageMultiplier = data.getDouble("damageMultiplier", 1.05);
    private final int minDistance = data.getInt("minDistance", 5);

    public HighGround(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    public double onAttack(LivingEntity target, double damage, DamageType type) {
        if ((target.getLocation().getY() + minDistance) < caster.getLocation().getY()) {
            return damage * damageMultiplier;
        }
        return super.onAttack(target, damage, type);
    }
}
