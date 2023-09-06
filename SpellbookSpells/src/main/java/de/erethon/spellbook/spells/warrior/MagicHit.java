package de.erethon.spellbook.spells.warrior;

import de.erethon.papyrus.DamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.paladin.PaladinBaseSpell;
import org.bukkit.entity.LivingEntity;

public class MagicHit extends PaladinBaseSpell {

    public MagicHit(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = 200;
    }

    @Override
    public double onAttack(LivingEntity target, double damage, DamageType type) {
        if (type == DamageType.PHYSICAL) {
            currentTicks = keepAliveTicks;
            return damage + Spellbook.getVariedAttributeBasedDamage(data, caster, target, true, org.bukkit.attribute.Attribute.ADV_MAGIC);
        }
        return super.onAttack(target, damage, type);
    }
}