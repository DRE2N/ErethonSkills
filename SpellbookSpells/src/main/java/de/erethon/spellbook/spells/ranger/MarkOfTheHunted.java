package de.erethon.spellbook.spells.ranger;

import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;

public class MarkOfTheHunted extends RangerBaseSpell {

    private final int effectDuration = data.getInt("effectDuration", 300);
    private final EffectData effectData = Bukkit.getServer().getSpellbookAPI().getLibrary().getEffectByID("MarkOfTheHuntedEffect");

    public MarkOfTheHunted(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onPrecast() {
        if (!lookForTarget()) {
            return false;
        }
        return super.onPrecast();
    }

    @Override
    protected boolean onCast() {
        target.addEffect(caster, effectData, effectDuration, 1);
        return super.onCast();
    }
}
