package de.erethon.spellbook.spells.warrior;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import org.bukkit.entity.LivingEntity;

public class ShoutFury extends AbstractWarriorShout {

    private final EffectData effectData = Spellbook.getEffectData("Fury");
    public ShoutFury(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    public boolean onCast() {
        for (LivingEntity living : caster.getLocation().getNearbyLivingEntities(range)) {
            if (living == caster) continue;
            if (Spellbook.canAttack(caster, living)) continue;
            living.addEffect(caster, effectData, data.getInt("duration", 200), data.getInt("stacks", 1));
        }
        return super.onCast();
    }
}
