package de.erethon.spellbook.traits.warrior;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.api.TraitData;
import de.erethon.spellbook.traits.ClassMechanic;
import org.bukkit.entity.LivingEntity;

public class BruiserRageMechanic extends ClassMechanic {

    private final int ragePerHit = data.getInt("ragePerHit", 1);

    public BruiserRageMechanic(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onAdd() {
        caster.setMaxEnergy(100);
    }

    @Override
    public double onDamage(LivingEntity target, double damage, PDamageType type) {
        caster.addEnergy(ragePerHit);
        return super.onAttack(target, damage, type);
    }
}
