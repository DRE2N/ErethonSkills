package de.erethon.spellbook.traits.assassin;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import de.erethon.spellbook.spells.PassiveSpell;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;

public class AssassinsAreNotTanky extends SpellTrait {

    private final double bonusDamage = caster.getAttribute(Attribute.ADVANTAGE_PHYSICAL).getValue() * data.getDouble("damageCoefficient", 0.7);
    private double threshold = data.getDouble("threshold", 0.8);

    public AssassinsAreNotTanky(TraitData traitData, LivingEntity caster) {
        super(traitData, caster);
    }

    @Override
    public double onAttack(LivingEntity target, double damage, PDamageType type) {
        double maxHealth = caster.getAttribute(Attribute.MAX_HEALTH).getValue();
        if (caster.getHealth() > (maxHealth * threshold)) {
            damage = damage + bonusDamage;
        }
        damage = damage + bonusDamage;
        return super.onAttack(target, damage, type);
    }
}
