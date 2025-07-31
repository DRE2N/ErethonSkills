package de.erethon.spellbook.traits.paladin.hierophant;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;

public class HierophantBasicAttack extends SpellTrait {

    // The Hierophant's basic attack generates wrath on damage taken and dealt, and increases magic damage based on the wrath accumulated.

    private final double wrathPerDamageTaken = data.getDouble("wrathPerDamageTaken", 0.2);
    private final double wrathPerDamageDealt = data.getDouble("wrathPerDamageDealt", 0.1);
    private final double magicDamageBonusPerWrath = data.getDouble("magicDamageBonusPerWrath", 1);

    public HierophantBasicAttack(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onAdd() {
        super.onAdd();
        caster.setMaxEnergy(100);
    }

    @Override
    public double onDamage(LivingEntity attacker, double damage, PDamageType type) {
        if (attacker != null) {
            caster.addEnergy((int) (damage * wrathPerDamageTaken));
            caster.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, caster.getLocation(), 3, 0.5, 0.5, 0.5);
        }
        return super.onDamage(attacker, damage, type);
    }

    @Override
    public double onAttack(LivingEntity target, double damage, PDamageType type) {
        if (target != null) {
            caster.addEnergy((int) (damage * wrathPerDamageDealt));
            damage += magicDamageBonusPerWrath * caster.getEnergy();
            caster.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, caster.getLocation(), 3, 0.5, 0.5, 0.5);
        }
        return super.onAttack(target, damage, type);
    }
}
