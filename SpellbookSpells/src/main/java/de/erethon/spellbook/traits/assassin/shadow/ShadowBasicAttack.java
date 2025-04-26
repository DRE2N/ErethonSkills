package de.erethon.spellbook.traits.assassin.shadow;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;

public class ShadowBasicAttack extends SpellTrait {

    // Basic attack trait for the Shadow class. Grants bonus energy and damage when attacking from behind.

    private final double backAngleForBonus = data.getInt("backAngleForBonus", 40);
    private final int bonusEnergy = data.getInt("bonusEnergy", 5);
    private final double bonusDamageMultiplier = data.getDouble("bonusDamageMultiplier", 1.2);

    public ShadowBasicAttack(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    public double onAttack(LivingEntity target, double damage, PDamageType type) {
        double angle = Math.abs(caster.getLocation().getYaw() - target.getLocation().getYaw());
        // If the caster is not behind the target, just return the damage
        if (angle > backAngleForBonus) {
            return super.onAttack(target, damage, type);
        }
        caster.addEnergy(bonusEnergy);
        damage *= bonusDamageMultiplier;
        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.6f, 2.0f);
        caster.getWorld().spawnParticle(Particle.DRIPPING_LAVA, target.getLocation(), 3, 0.5, 0.5, 0.5);
        return damage;
    }
}
