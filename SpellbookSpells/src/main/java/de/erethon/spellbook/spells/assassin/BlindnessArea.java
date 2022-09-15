package de.erethon.spellbook.spells.assassin;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.AoEBaseSpell;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;

public class BlindnessArea extends AoEBaseSpell {

    EffectData effectData = Bukkit.getServer().getSpellbookAPI().getLibrary().getEffectByID("Blindness");

    public BlindnessArea(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }


    @Override
    public boolean onPrecast() {
        return super.onPrecast() && AssassinUtils.hasEnergy(caster, data);
    }

    @Override
    protected boolean onCast() {
        keepAliveTicks = 2;
        return super.onCast();
    }

    @Override
    protected void onAfterCast() {
        caster.removeEnergy(data.getInt("energyCost", 0));
    }

    @Override
    public void onTick() {
        super.onTick();
        int duration = data.getInt("effectDuration", 200);
        for (LivingEntity entity : getEntities()) {
            entity.addEffect(caster, effectData, duration, (int) Spellbook.getScaledValue(data, caster, Attribute.ADV_MAGIC));
        }
    }

    @Override
    protected void onTickFinish() {
        super.onTickFinish();
    }
}
