package de.erethon.spellbook.traits.ranger;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import de.erethon.spellbook.utils.RangerUtils;
import org.bukkit.entity.LivingEntity;

public class PetFighter extends SpellTrait {

    private final int bonusDamage = data.getInt("bonusDamage", 20);

    public PetFighter(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    public double onAttack(LivingEntity target, double damage, PDamageType type) {
        if (!RangerUtils.hasPet(caster) || RangerUtils.getPet(caster).getHealth() >= RangerUtils.getPet(caster).getMaxHealth() / 2) {
            return super.onAttack(target, damage, type);
        }
        return damage + bonusDamage;
    }
}
