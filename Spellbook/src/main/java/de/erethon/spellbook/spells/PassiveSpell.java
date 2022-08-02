package de.erethon.spellbook.spells;

import de.erethon.spellbook.SpellData;
import de.erethon.spellbook.SpellbookSpell;
import de.erethon.spellbook.caster.SpellCaster;
import org.bukkit.event.Listener;

public abstract class PassiveSpell extends SpellbookSpell implements Listener {

    public PassiveSpell(SpellCaster caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = -1;
        data.getSpellbook().getImplementingPlugin().getServer().getPluginManager().registerEvents(this, data.getSpellbook().getImplementingPlugin());
    }

}

