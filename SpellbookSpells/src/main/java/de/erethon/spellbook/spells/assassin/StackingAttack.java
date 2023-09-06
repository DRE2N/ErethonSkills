package de.erethon.spellbook.spells.assassin;

import de.erethon.papyrus.DamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookSpell;
import de.erethon.spellbook.utils.AssassinUtils;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

public class StackingAttack extends AssassinBaseSpell {

    int stacks;

    public StackingAttack(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = spellData.getInt("duration", 10) * 20;
    }

    @Override
    protected boolean onPrecast() {
        return super.onPrecast() && lookForTarget();
    }

    @Override
    public double onAttack(LivingEntity target, double damage, DamageType type) {
        if (target != this.target) {
            return damage;
        }
        damage = damage + (Spellbook.getScaledValue(data, caster, target, Attribute.ADV_AIR) * stacks);
        if (stacks <= data.getInt("maxStacks", 7)) {
            stacks++;
        }
        return super.onAttack(target, damage, type);
    }
}
