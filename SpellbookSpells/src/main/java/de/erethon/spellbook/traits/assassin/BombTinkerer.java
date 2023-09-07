package de.erethon.spellbook.traits.assassin;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import de.erethon.spellbook.api.TraitTrigger;
import org.bukkit.entity.LivingEntity;

public class BombTinkerer extends SpellTrait {

    private final int durationFury = data.getInt("fury.duration", 120);
    private final int stacksFury = data.getInt("fury.stacks", 1);
    private final int durationPower = data.getInt("power.duration", 120);
    private final int stacksPower = data.getInt("power.stacks", 1);

    private final EffectData fury = Spellbook.getEffectData("Fury");
    private final EffectData power = Spellbook.getEffectData("Power");

    public BombTinkerer(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onTrigger(TraitTrigger trigger) {
        if (trigger.getId() != 2) return;
        caster.addEffect(caster, fury, durationFury, stacksFury);
        caster.addEffect(caster, power, durationPower, stacksPower);
    }
}
