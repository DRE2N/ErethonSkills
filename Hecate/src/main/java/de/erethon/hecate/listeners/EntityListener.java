package de.erethon.hecate.listeners;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.hecate.Hecate;
import de.erethon.hecate.ui.EntityStatusDisplay;
import de.erethon.hecate.ui.EntityStatusDisplayManager;
import de.erethon.spellbook.api.SpellEffect;
import de.erethon.spellbook.api.SpellEffectAddEvent;
import de.erethon.spellbook.api.SpellEffectRemoveEvent;
import de.erethon.spellbook.teams.TeamManager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class EntityListener implements Listener {

    private final EntityStatusDisplayManager displayManager = Hecate.getInstance().getStatusDisplayManager();
    private final TeamManager teamManager = Hecate.getInstance().getSpellbook().getTeamManager();

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (displayManager.hasStatusDisplay(event.getEntity())) {
            return;
        }
        displayManager.addStatusDisplay(event.getEntity(), new EntityStatusDisplay((LivingEntity) event.getEntity()));
        event.getEntity().setCustomNameVisible(true);
    }
    @EventHandler
    public void onEntityAdd(EntityAddToWorldEvent event) {
        if (event.getEntity() instanceof LivingEntity living) {
            teamManager.loadEntity(living);
        }
    }

    // We need to clear effects in case someone logs out or is despawned while having a buff active
    @EventHandler
    public void onEntityRemove(EntityRemoveFromWorldEvent event) {
        if (event.getEntity() instanceof LivingEntity living) {
            living.getEffects().forEach(SpellEffect::onRemove);
            if (displayManager.hasStatusDisplay(living)) {
                displayManager.removeStatusDisplay(living);
            }
        }
    }

    @EventHandler
    public void onLogout(PlayerQuitEvent event) {
        event.getPlayer().getEffects().forEach(SpellEffect::onRemove);
    }

    @EventHandler
    public void onEffectAdd(SpellEffectAddEvent event) {
        LivingEntity entity = (LivingEntity) event.getTarget();
        if (!displayManager.hasStatusDisplay(entity)) {
            displayManager.addStatusDisplay(entity, new EntityStatusDisplay(entity));
        }
        displayManager.getStatusDisplay(entity).updateStatusDisplay();
    }

    @EventHandler
    public void onEffectRemove(SpellEffectRemoveEvent event) {
        LivingEntity entity = (LivingEntity) event.getTarget();
        if (!displayManager.hasStatusDisplay(entity)) {
            displayManager.addStatusDisplay(entity, new EntityStatusDisplay(entity));
        }
        displayManager.getStatusDisplay(entity).updateStatusDisplay();
    }
}
