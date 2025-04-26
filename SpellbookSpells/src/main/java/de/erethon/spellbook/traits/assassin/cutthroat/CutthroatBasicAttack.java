package de.erethon.spellbook.traits.assassin.cutthroat;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;

public class CutthroatBasicAttack extends SpellTrait {

    // Every 3rd attack grants bonus energy and damage. The bonus damage is multiplied by a factor of 1.2.

    private final int attacksForBonus = data.getInt("attacksForBonus", 3);
    private final int bonusEnergy = data.getInt("bonusEnergy", 5);
    private final double bonusDamageMultiplier = data.getDouble("bonusDamageMultiplier", 1.2);

    private int currentAttacks = 0;

    public CutthroatBasicAttack(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    public double onAttack(LivingEntity target, double damage, PDamageType type) {
        currentAttacks++;
        if (currentAttacks >= attacksForBonus) {
            currentAttacks = 0;
            caster.addEnergy(bonusEnergy);
            damage *= bonusDamageMultiplier;
            caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.6f, 2.0f);
            caster.getWorld().spawnParticle(Particle.DRIPPING_LAVA, target.getLocation(), 3, 0.5, 0.5, 0.5);
        }
        return super.onAttack(target, damage, type);
    }
}
