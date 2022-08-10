package de.erethon.spellbook.spells.assassin;

import de.erethon.papyrus.DamageType;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookSpell;
import org.bukkit.entity.LivingEntity;

public class DoubleAttack extends SpellbookSpell {

    public DoubleAttack(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = data.getInt("duration", 10) * 20;
    }

    @Override
    protected boolean onPrecast() {
        return AssassinUtils.hasEnergy(caster, data);
    }

    @Override
    protected void onAfterCast() {
        caster.removeEnergy(data.getInt("energyCost", 30));
    }

    @Override
    public double onAttack(LivingEntity target, double damage, DamageType type) {
        caster.attack(target);
        return super.onAttack(target, damage, type);
    }
}
