package de.erethon.spellbook.traits.assassin;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.entity.LivingEntity;

public class HeartFlicker extends SpellTrait {

    private final double damagePerMissingHealthPercent = data.getDouble("damagePerMissingHealthPercent", 1.0);

    public HeartFlicker(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    public double onAttack(LivingEntity target, double damage, PDamageType type) {
        double missingHealthPercent = (caster.getMaxHealth() - caster.getHealth()) / caster.getMaxHealth();
        return damage + missingHealthPercent * damagePerMissingHealthPercent;
    }
}

