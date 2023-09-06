package de.erethon.spellbook.spells.assassin;

import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookSpell;
import de.erethon.spellbook.utils.AssassinUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;

public class SpeedBurst extends AssassinBaseSpell {

    public SpeedBurst(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onCast() {
        EffectData effectData = Bukkit.getServer().getSpellbookAPI().getLibrary().getEffectByID("Speed");
        caster.addEffect(caster, effectData, data.getInt("duration", 10) * 20, 1);
        return super.onCast();
    }

}

