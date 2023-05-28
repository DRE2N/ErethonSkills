package de.erethon.hecate.listeners;

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import de.erethon.spellbook.api.SpellEffect;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class EntityListener implements Listener {

    // We need to clear effects in case someone logs out or is despawned while having a buff active
    @EventHandler
    public void onEntityRemove(EntityRemoveFromWorldEvent event) {
        if (event.getEntity() instanceof LivingEntity living) {
            living.getEffects().forEach(SpellEffect::onRemove);
        }
    }

    @EventHandler
    public void onLogout(PlayerQuitEvent event) {
        event.getPlayer().getEffects().forEach(SpellEffect::onRemove);
    }
}
