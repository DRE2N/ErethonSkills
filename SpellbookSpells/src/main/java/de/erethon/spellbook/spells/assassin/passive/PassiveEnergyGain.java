package de.erethon.spellbook.spells.assassin.passive;

import de.erethon.papyrus.DamageType;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.PassiveSpell;
import org.bukkit.entity.LivingEntity;

public class PassiveEnergyGain extends PassiveSpell {
    public PassiveEnergyGain(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    public double onAttack(SpellCaster target, double damage, DamageType type) {
        caster.addEnergy((int) (damage / 2));
        return damage;
    }

}
