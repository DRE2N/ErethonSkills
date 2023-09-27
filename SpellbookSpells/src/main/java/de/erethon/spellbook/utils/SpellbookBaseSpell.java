package de.erethon.spellbook.utils;

import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookSpell;
import org.bukkit.entity.LivingEntity;

public abstract class SpellbookBaseSpell extends SpellbookSpell implements Targeted {

    public SpellbookBaseSpell(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    public boolean onCast() {
        return super.onCast();
    }

}
