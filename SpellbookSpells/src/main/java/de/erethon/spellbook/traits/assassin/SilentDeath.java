package de.erethon.spellbook.traits.assassin;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.entity.LivingEntity;

public class SilentDeath extends SpellTrait {

    private final double damageMultiplier = data.getDouble("damageMultiplier", 1.5);
    private final double singleTargetRange = data.getDouble("singleTargetRange", 16.0);

    public SilentDeath(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    public double onAttack(LivingEntity target, double damage, PDamageType type) {
        int i = 0;
        for (LivingEntity living : target.getLocation().getNearbyLivingEntities(singleTargetRange)) {
            if (Spellbook.canAttack(caster, living)) {
                i++;
            }
        }
        if (i == 0) {
            damage *= damageMultiplier;
        }
        return damage;
    }
}
