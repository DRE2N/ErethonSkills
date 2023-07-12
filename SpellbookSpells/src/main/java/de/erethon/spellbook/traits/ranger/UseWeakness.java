package de.erethon.spellbook.traits.ranger;

import de.erethon.papyrus.DamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.entity.LivingEntity;

public class UseWeakness extends SpellTrait {

    private final EffectData weakness = Spellbook.getInstance().getAPI().getLibrary().getEffectByID("weakness");

    private double bonusDamage = data.getDouble("bonusDamage", 0.2);

    public UseWeakness(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    public double onAttack(LivingEntity target, double damage, DamageType type) {
        if (target.hasEffect(weakness)) {
            return super.onAttack(target, damage + (damage * bonusDamage), type);
        }
        return super.onAttack(target, damage, type);
    }
}
