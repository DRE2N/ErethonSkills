package de.erethon.spellbook;

import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookSpell;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class ChannelingSpell extends SpellbookSpell implements Listener {

    protected boolean interrupted = false;

    public ChannelingSpell(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        Bukkit.getServer().getPluginManager().registerEvents(this, Spellbook.getInstance().getImplementer());
    }

    protected void onInterrupt() {}

    @EventHandler
    private void onMove(PlayerMoveEvent event) {
        if (event.getPlayer() != caster || !event.hasChangedPosition()) {
            return;
        }
        interrupted = true;
    }

    @Override
    protected void onTickFinish() {
        super.onTickFinish();
        HandlerList.unregisterAll(this);
    }
}
