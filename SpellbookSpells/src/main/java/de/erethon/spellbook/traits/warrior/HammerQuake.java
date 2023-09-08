package de.erethon.spellbook.traits.warrior;

import de.erethon.papyrus.DamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.entity.LivingEntity;

import java.util.Random;

public class HammerQuake extends SpellTrait {

    private final double stunChance = data.getDouble("stunChance", 0.1);
    private final int stunDuration = data.getInt("stunDuration", 20);
    private final EffectData effectData = Spellbook.getEffectData("Stun");
    private final Random random = new Random();

    public HammerQuake(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    public double onAttack(LivingEntity target, double damage, DamageType type) {
        if (random.nextDouble() < stunChance) {
            target.addEffect(caster, effectData, stunDuration, 1);
        }
        return super.onAttack(target, damage, type);
    }
}
