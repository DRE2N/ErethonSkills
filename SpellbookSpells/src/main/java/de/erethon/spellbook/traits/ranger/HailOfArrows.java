package de.erethon.spellbook.traits.ranger;

import de.erethon.papyrus.DamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.entity.LivingEntity;

public class HailOfArrows extends SpellTrait {

    private final int duration = data.getInt("duration", 20);
    private final EffectData effectData = Spellbook.getEffectData("Fury");

    public HailOfArrows(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    public double onDamage(LivingEntity attacker, double damage, DamageType type) {
        caster.addEffect(caster, effectData, duration, 1);
        return super.onDamage(attacker, damage, type);
    }
}
