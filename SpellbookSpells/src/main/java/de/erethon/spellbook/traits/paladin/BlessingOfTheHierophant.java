package de.erethon.spellbook.traits.paladin;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import de.erethon.spellbook.api.TraitTrigger;
import org.bukkit.entity.LivingEntity;

public class BlessingOfTheHierophant extends SpellTrait {

    private final int powerDuration = data.getInt("power.duration", 100);
    private final int powerStacks = data.getInt("power.stacks", 3);
    private final int furyDuration = data.getInt("fury.duration", 100);
    private final int furyStacks = data.getInt("fury.stacks", 3);
    private final EffectData power = Spellbook.getEffectData("Power");
    private final EffectData fury = Spellbook.getEffectData("Fury");

    public BlessingOfTheHierophant(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onTrigger(TraitTrigger trigger) {
        for (LivingEntity living : trigger.getTargets()) {
            living.addEffect(caster, power, powerDuration, powerStacks);
            living.addEffect(caster, fury, furyDuration, furyStacks);
        }
    }
}
