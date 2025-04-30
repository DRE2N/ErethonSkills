package de.erethon.spellbook.traits.paladin.guardian;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.entity.LivingEntity;

public class GuardianBasicAttack extends SpellTrait {

    private final int devotionPerAttack = data.getInt("devotionPerAttack", 5);
    private final int devotionPerHeal = data.getInt("devotionPerHeal", 15);

    private int currentDevotion = 0;

    public GuardianBasicAttack(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    public double onAttack(LivingEntity target, double damage, PDamageType type) {
        if (target != null) {
            caster.setEnergy(caster.getEnergy() + devotionPerAttack);
            currentDevotion += devotionPerAttack;
            if (currentDevotion >= devotionPerHeal) {
                currentDevotion = 0;
                caster.heal(devotionPerHeal);
            }
        }
        return super.onAttack(target, damage, type);
    }
}
