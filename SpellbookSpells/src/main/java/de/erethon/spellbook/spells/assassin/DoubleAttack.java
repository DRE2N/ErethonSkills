package de.erethon.spellbook.spells.assassin;

import de.erethon.papyrus.DamageType;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookSpell;
import de.erethon.spellbook.utils.AssassinUtils;
import org.bukkit.entity.LivingEntity;

public class DoubleAttack extends SpellbookSpell {

    public DoubleAttack(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = data.getInt("duration", 10) * 20;
    }

    @Override
    public double onAttack(LivingEntity target, double damage, DamageType type) {
        //caster.attack(target); TODO
        return super.onAttack(target, damage, type);
    }
}
