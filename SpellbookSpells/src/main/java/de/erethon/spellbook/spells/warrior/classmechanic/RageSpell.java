package de.erethon.spellbook.spells.warrior.classmechanic;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellCastEvent;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookSpell;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class RageSpell extends SpellbookSpell implements Listener {

    private final int rageLevelRequired = data.getInt("requiredRageLevel", 99);
    private final SpellData spellToOverload = Bukkit.getServer().getSpellbookAPI().getLibrary().getSpellByID(data.getString("spellToOverload"));

    public RageSpell(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        if (spellToOverload == null) {
            Bukkit.getLogger().warning("Spell overload" + data.getString("spellToOverload") + " not found.");
            this.failed = true;
        }
        Bukkit.getPluginManager().registerEvents(this, Spellbook.getInstance().getImplementer());
    }

    @EventHandler
    public void onSpellCast(SpellCastEvent event) {
        if (spellToOverload != null) {
            if (event.getData() != spellToOverload) return;
            event.setCancelled(true);
            event.getActiveSpell().cancel();
            spellToOverload.queue(event.getCaster());
        }
    }



}
