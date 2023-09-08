package de.erethon.spellbook.traits.warrior;

import de.erethon.papyrus.DamageType;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.entity.LivingEntity;

import java.util.Random;

public class Painless extends SpellTrait {

    private final double missChance = data.getDouble("missChance", 0.05);
    private final Random random = new Random();

    public Painless(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    public double onDamage(LivingEntity attacker, double damage, DamageType type) {
        if (random.nextDouble() < missChance) {
            return 0;
        }
        return super.onDamage(attacker, damage, type);
    }
}
