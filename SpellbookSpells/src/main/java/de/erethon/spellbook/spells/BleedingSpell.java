package de.erethon.spellbook.spells;

import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.entity.LivingEntity;

public class BleedingSpell extends EntityTargetSpell {

    public BleedingSpell(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    public boolean onPrecast() {
        return super.onPrecast();
    }

    @Override
    public boolean onCast() {
        targetEntity.getWorld().playEffect(targetEntity.getLocation(), Effect.GHAST_SHRIEK, 1);
        EffectData data  = Bukkit.getServer().getSpellbookAPI().getLibrary().getEffectByID("Fear");
        targetEntity.addEffect(caster, data,data.getInt("duration", 10), 1);
        return true;
    }

    @Override
    public void onAfterCast() {
        caster.setCooldown(data);
    }

}

