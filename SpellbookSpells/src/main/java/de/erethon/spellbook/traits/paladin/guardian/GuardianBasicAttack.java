package de.erethon.spellbook.traits.paladin.guardian;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.entity.LivingEntity;

public class GuardianBasicAttack extends SpellTrait {

    // The Guardian's basic attack generates devotion points on hit and heals the caster when enough devotion is accumulated.

    private final int devotionPerAttack = data.getInt("devotionPerAttack", 5);
    private final int devotionPerHeal = data.getInt("devotionPerHeal", 15);

    private int currentDevotion = 0;

    public GuardianBasicAttack(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onAdd() {
        super.onAdd();
        caster.setMaxEnergy(100);
    }

    @Override
    public double onAttack(LivingEntity target, double damage, PDamageType type) {
        if (target != null) {
            caster.addEnergy(devotionPerAttack);
            currentDevotion += devotionPerAttack;
            if (currentDevotion >= devotionPerHeal) {
                currentDevotion = 0;
                caster.heal(devotionPerHeal);
            }
        }
        return super.onAttack(target, damage, type);
    }
}
