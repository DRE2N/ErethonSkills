package de.erethon.spellbook.traits.assassin;

import de.erethon.papyrus.DamageType;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import de.erethon.spellbook.spells.PassiveSpell;
import de.erethon.spellbook.utils.BlockFaceWrapper;
import org.bukkit.entity.LivingEntity;

/**
 * @author Fyreum
 */
public class DaggerThrust extends SpellTrait {

    public DaggerThrust(TraitData traitData, LivingEntity caster) {
        super(traitData, caster);
    }


    @Override
    public double onAttack(LivingEntity target, double damage, DamageType type) {
        if (BlockFaceWrapper.isSimilar(target.getFacing(), caster.getFacing())) {
            damage = damage * (1 + data.getDouble("multiplier", 0.3));
        }
        return super.onAttack(target, damage, type);
    }
}
