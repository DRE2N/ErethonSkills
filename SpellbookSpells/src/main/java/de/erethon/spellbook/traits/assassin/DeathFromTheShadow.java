package de.erethon.spellbook.traits.assassin;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import de.erethon.spellbook.spells.PassiveSpell;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * @author Fyreum
 */
public class DeathFromTheShadow extends SpellTrait {

    public DeathFromTheShadow(TraitData traitData, LivingEntity caster) {
        super(traitData, caster);
    }

    @Override
    public double onAttack(LivingEntity target, double damage, PDamageType type) {
        if (!caster.isInvisible()) {
            return damage;
        }
        return super.onAttack(target, damage * data.getDouble("multiplier", 0.3), type);
    }
}
