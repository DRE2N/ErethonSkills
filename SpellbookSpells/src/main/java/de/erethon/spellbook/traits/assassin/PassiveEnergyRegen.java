package de.erethon.spellbook.traits.assassin;

import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import de.erethon.spellbook.spells.PassiveSpell;
import org.bukkit.entity.LivingEntity;

public class PassiveEnergyRegen extends SpellTrait {

    private final int energy = data.getInt("energy", 1);

    public PassiveEnergyRegen(TraitData traitData, LivingEntity caster) {
        super(traitData, caster);
    }


    @Override
    protected void onTick() {
        caster.addEnergy(energy);
    }
}

