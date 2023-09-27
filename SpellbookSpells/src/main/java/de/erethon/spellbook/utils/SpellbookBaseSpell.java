package de.erethon.spellbook.utils;

import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookSpell;
import org.bukkit.entity.LivingEntity;

public class SpellbookBaseSpell extends SpellbookSpell implements Targeted {

    public SpellbookBaseSpell(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    public boolean onCast() {
        return super.onCast();
    }

    @Override
    public LivingEntity getTarget() {
        return null;
    }

    @Override
    public void setTarget(LivingEntity target) {
    }
}
