package de.erethon.spellbook.spells.ranger;

import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookSpell;
import io.papermc.paper.event.entity.EntityMoveEvent;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class NatureWalk extends SpellbookSpell implements Listener {

    int duration = data.getInt("duration", 400);
    int interval = data.getInt("interval", 2);

    public NatureWalk(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        tickInterval = interval;
        keepAliveTicks = duration;
    }

    @EventHandler
    private void onMove(PlayerMoveEvent event) {
        if (event.getPlayer() != caster || !event.hasChangedBlock()) {
            return;
        }
    }
    @EventHandler
    private void onEntityMove(EntityMoveEvent event) {
        if (event.getEntity() != caster || !event.hasChangedBlock()) {
            return;
        }

    }


}
