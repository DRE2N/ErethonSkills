package de.erethon.spellbook.spells.warrior;

import de.erethon.papyrus.DamageType;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookSpell;
import org.bukkit.entity.LivingEntity;

public class FireSword extends SpellbookSpell {

    protected int duration = 200;
    protected int BonusDamage= 10;

    public FireSword(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = duration;
    }
    public double onDamage(LivingEntity target, double damage, DamageType type) {
        if (type == DamageType.MAGIC) {
            return super.onDamage(target, damage, type);
        }
        target.damage(BonusDamage, DamageType.MAGIC);
        return super.onDamage(target, damage + BonusDamage, type);
    }
}



