package de.erethon.spellbook.spells.assassin.passive;

import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.PassiveSpell;
import org.bukkit.entity.LivingEntity;

public class PassiveEnergyRegen extends PassiveSpell {

    private final int energy = data.getInt("energy", 1);

    public PassiveEnergyRegen(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        tickInterval = data.getInt("interval", 5) * 20;
    }

    @Override
    protected void onTick() {
        caster.addEnergy(energy);
    }
}

