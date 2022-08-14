package de.erethon.spellbook.spells.assassin.passive;

import de.erethon.papyrus.DamageType;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.PassiveSpell;
import de.erethon.spellbook.utils.BlockFaceWrapper;
import org.bukkit.entity.LivingEntity;

/**
 * @author Fyreum
 */
public class DaggerThrust extends PassiveSpell {

    public DaggerThrust(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    public double onAttack(LivingEntity target, double damage, DamageType type) {
        if (BlockFaceWrapper.isSimilar(target.getFacing(), caster.getFacing())) {
            damage = damage * (1 + data.getDouble("multiplier", 0.3));
        }
        return super.onAttack(target, damage, type);
    }
}
