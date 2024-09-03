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
    private int currentTicks = 0;
    protected int channelTime;

    public ChannelingSpell(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        channelTime = spellData.getInt("channelTime", 60);
        Bukkit.getServer().getPluginManager().registerEvents(this, Spellbook.getInstance().getImplementer());
    }

    protected void onInterrupt() {}

    protected void onChannelFinish() {}

    @Override
    protected void onTick() {
        super.onTick();
        currentTicks++;
        caster.sendParsedActionBar("<color:#ff0000>Channeling... <color:#ffffff>" + currentTicks + " / " + channelTime);
        if(currentTicks >= channelTime) {
            onChannelFinish();
        }
    }

    @EventHandler
    private void onMove(PlayerMoveEvent event) {
        if (event.getPlayer() != caster || !event.hasChangedPosition()) {
            return;
        }
        interrupted = true;
        onInterrupt();
    }

    @Override
    protected void onTickFinish() {
        super.onTickFinish();
        HandlerList.unregisterAll(this);
    }
}
