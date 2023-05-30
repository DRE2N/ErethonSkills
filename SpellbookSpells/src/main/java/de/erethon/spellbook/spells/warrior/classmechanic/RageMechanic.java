package de.erethon.spellbook.spells.warrior.classmechanic;

import de.erethon.papyrus.DamageType;
import de.erethon.spellbook.api.TraitData;
import de.erethon.spellbook.spells.ClassMechanic;
import org.bukkit.entity.LivingEntity;

public class RageMechanic extends ClassMechanic {

    private final int ragePerHit = data.getInt("ragePerHit", 1);

    public RageMechanic(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onAdd() {
        caster.setMaxEnergy(100);
    }

    @Override
    public double onAttack(LivingEntity target, double damage, DamageType type) {
        caster.addEnergy(ragePerHit);
        return super.onAttack(target, damage, type);
    }
}
