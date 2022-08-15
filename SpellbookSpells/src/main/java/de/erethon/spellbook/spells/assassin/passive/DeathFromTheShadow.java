package de.erethon.spellbook.spells.assassin.passive;

import de.erethon.papyrus.DamageType;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.PassiveSpell;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * @author Fyreum
 */
public class DeathFromTheShadow extends PassiveSpell {

    public DeathFromTheShadow(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    public double onAttack(LivingEntity target, double damage, DamageType type) {
        if (target.hasLineOfSight(caster)) {
            return super.onAttack(target, damage, type);
        }
        for (Player tracked : caster.getTrackedPlayers()) {
            // todo: Look at faction state/hostility
            if (tracked.hasLineOfSight(caster)) {
                return super.onAttack(target, damage, type);
            }
        }
        return super.onAttack(target, damage * data.getDouble("multiplier", 0.3), type);
    }
}
