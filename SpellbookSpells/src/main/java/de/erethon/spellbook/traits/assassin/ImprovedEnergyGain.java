package de.erethon.spellbook.traits.assassin;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import de.erethon.spellbook.spells.PassiveSpell;
import org.bukkit.entity.LivingEntity;

public class ImprovedEnergyGain extends SpellTrait {

    private final int bonus = data.getInt("bonus", 2);

    public ImprovedEnergyGain(TraitData traitData, LivingEntity caster) {
        super(traitData, caster);
    }

    @Override
    protected void onAdd() {
        caster.setMaxEnergy(100);
    }

    @Override
    public double onAttack(LivingEntity target, double damage, PDamageType type) {
        caster.addEnergy((int) (damage / 2) + bonus);
        return damage;
    }

    @Override
    protected void onRemove() {
        caster.setMaxEnergy(0);
    }
}
