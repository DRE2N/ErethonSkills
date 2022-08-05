package de.erethon.spellbook.spells;

import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookSpell;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Listener;

public abstract class PassiveSpell extends SpellbookSpell implements Listener {

    public PassiveSpell(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = -1;
        //data.getSpellbook().getServer().getPluginManager().registerEvents(this, data.getSpellbook().getImplementingPlugin());
    }

}

