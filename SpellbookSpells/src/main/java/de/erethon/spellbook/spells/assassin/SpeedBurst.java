package de.erethon.spellbook.spells.assassin;

import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookSpell;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;

public class SpeedBurst extends SpellbookSpell {

    public SpeedBurst(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onPrecast() {
        return  AssassinUtils.hasEnergy(caster, data);
    }

    @Override
    protected boolean onCast() {
        EffectData effectData = Bukkit.getServer().getSpellbookAPI().getLibrary().getEffectByID("Speed");
        caster.addEffect(caster, effectData, data.getInt("duration", 10) * 20, 1);
        return super.onCast();
    }

    @Override
    protected void onAfterCast() {
        caster.removeEnergy(data.getInt("energyCost", 0));
    }
}

