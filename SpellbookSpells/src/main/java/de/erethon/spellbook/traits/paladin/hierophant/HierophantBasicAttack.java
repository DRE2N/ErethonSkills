package de.erethon.spellbook.traits.paladin.hierophant;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;

public class HierophantBasicAttack extends SpellTrait {

    private final double wrathPerDamageTaken = data.getDouble("wrathPerDamageTaken", 0.2);
    private final double wrathPerDamageDealt = data.getDouble("wrathPerDamageDealt", 0.1);
    private final double magicDamageBonusPerWrath = data.getDouble("magicDamageBonusPerWrath", 1);

    public HierophantBasicAttack(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    public double onDamage(LivingEntity attacker, double damage, PDamageType type) {
        if (attacker != null) {
            caster.setEnergy((int) (attacker.getEnergy() + (damage * wrathPerDamageTaken)));
            caster.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, caster.getLocation(), 3, 0.5, 0.5, 0.5);
        }
        return super.onDamage(attacker, damage, type);
    }

    @Override
    public double onAttack(LivingEntity target, double damage, PDamageType type) {
        if (target != null) {
            caster.setEnergy((int) (caster.getEnergy() + (damage * wrathPerDamageDealt)));
            damage += magicDamageBonusPerWrath * caster.getEnergy();
            caster.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, caster.getLocation(), 3, 0.5, 0.5, 0.5);
        }
        return super.onAttack(target, damage, type);
    }
}
