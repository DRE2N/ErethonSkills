package de.erethon.spellbook.spells.assassin.passive;

import de.erethon.papyrus.DamageType;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.PassiveSpell;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;

public class AssassinsAreNotTanky extends PassiveSpell {

    private double bonusDamage = data.getDouble("bonusDamage", 2.0);
    private double threshold = data.getDouble("threshold", 0.8);

    public AssassinsAreNotTanky(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    public double onAttack(LivingEntity target, double damage, DamageType type) {
        double maxHealth = caster.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
        if (caster.getHealth() > (maxHealth * threshold)) {
            damage = damage + bonusDamage;
        }
        damage = damage + bonusDamage;
        return super.onAttack(target, damage, type);
    }
}
