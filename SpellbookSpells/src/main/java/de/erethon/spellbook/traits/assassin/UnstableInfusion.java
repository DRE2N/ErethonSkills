package de.erethon.spellbook.traits.assassin;

import de.erethon.papyrus.DamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.entity.LivingEntity;

import java.util.Random;

public class UnstableInfusion extends SpellTrait {

    private final double poisonChance = data.getDouble("poisonChance", 0.1);
    private final int duration = data.getInt("duration", 120);
    private final int stacks = data.getInt("stacks", 1);
    private final EffectData effectData = Spellbook.getEffectData("Poison");
    private final Random random = new Random();

    public UnstableInfusion(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    public double onAttack(LivingEntity target, double damage, DamageType type) {
        if (random.nextDouble() < poisonChance) {
            target.addEffect(caster, effectData, duration, stacks);
        }
        return super.onAttack(target, damage, type);
    }
}
