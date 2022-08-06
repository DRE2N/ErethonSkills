package de.erethon.spellbook.spells;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookSpell;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

public abstract class PassiveSpell extends SpellbookSpell implements Listener {

    public PassiveSpell(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        Plugin implementer = Spellbook.getInstance().getImplementer();
        keepAliveTicks = -1;
        implementer.getServer().getPluginManager().registerEvents(this, implementer);
    }

}

