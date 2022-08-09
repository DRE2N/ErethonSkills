package de.erethon.spellbook.spells.assassin.passive;

import de.erethon.papyrus.DamageType;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.PassiveSpell;
import org.bukkit.entity.LivingEntity;

public class ImprovedEnergyGain extends PassiveSpell {

    private final int bonus = data.getInt("bonus", 2);

    public ImprovedEnergyGain(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    public double onAttack(LivingEntity target, double damage, DamageType type) {
        caster.addEnergy((int) (damage / 2) + bonus);
        return damage;
    }

}
