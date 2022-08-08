package de.erethon.spellbook.spells;

import de.erethon.papyrus.DamageType;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;

/**
 * @author Fyreum
 */
public class LifeStealPassiveSpell extends PassiveSpell {

    double percentage;

    public LifeStealPassiveSpell(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        percentage = data.getDouble("percentage", 0.1D);
    }


    @Override
    public double onDamage(LivingEntity attacker, double damage, DamageType type) {
        if (damage <= 0) {
            return damage;
        }
        double maxHealth = caster.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
        double health = caster.getHealth();
        if (health >= maxHealth) {
            return damage;
        }
        caster.setHealth(Math.min(health + damage * percentage, maxHealth));
        return damage;
    }

}
