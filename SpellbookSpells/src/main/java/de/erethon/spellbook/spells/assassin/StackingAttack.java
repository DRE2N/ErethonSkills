package de.erethon.spellbook.spells.assassin;

import de.erethon.papyrus.DamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookSpell;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

public class StackingAttack extends SpellbookSpell {

    LivingEntity target;
    int stacks;

    public StackingAttack(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = spellData.getInt("duration", 10) * 20;
    }

    @Override
    protected boolean onPrecast() {
        Entity selected = caster.getTargetEntity(data.getInt("range", 10));
        if (selected instanceof LivingEntity entity) {
            target = entity;
            return AssassinUtils.hasEnergy(caster, data);
        } else {
            caster.sendParsedActionBar("<color:#ff0000>Kein gültiges Ziel!");
            return false;
        }
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
