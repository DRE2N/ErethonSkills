package de.erethon.hecate.listeners;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.hecate.Hecate;
import de.erethon.hecate.ui.EntityStatusDisplay;
import de.erethon.hecate.ui.EntityStatusDisplayManager;
import de.erethon.spellbook.api.SpellChannelFinishEvent;
import de.erethon.spellbook.api.SpellChannelStartEvent;
import de.erethon.spellbook.api.SpellEffect;
import de.erethon.spellbook.api.SpellEffectAddEvent;
import de.erethon.spellbook.api.SpellEffectRemoveEvent;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.SpellbookSpell;
import de.erethon.spellbook.teams.TeamManager;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;

public class EntityListener implements Listener {

    private final EntityStatusDisplayManager displayManager = Hecate.getInstance().getStatusDisplayManager();
    private final TeamManager teamManager = Hecate.getInstance().getSpellbook().getTeamManager();

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof LivingEntity living)  {
            if (!displayManager.hasStatusDisplay(event.getEntity())) {
                displayManager.addStatusDisplay(living, new EntityStatusDisplay(living));
            }
            displayManager.getStatusDisplay(living).updateHealthDisplay(event.getDamage());
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (displayManager.hasStatusDisplay(event.getEntity())) {
            return;
        }
        if (event.getEntity() instanceof LivingEntity living) {
            displayManager.addStatusDisplay(living, new EntityStatusDisplay(living));
            event.getEntity().setCustomNameVisible(true);
        }
    }
    @EventHandler
    public void onEntityAdd(EntityAddToWorldEvent event) {
        if (event.getEntity() instanceof LivingEntity living) {
            teamManager.loadEntity(living);
        }
    }

    // We need to clear things in case someone logs out or is despawned while having a buff active
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
    public void onLogin(PlayerJoinEvent event) {
        event.getPlayer().displayName(Component.empty());
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if (displayManager.hasStatusDisplay(event.getEntity())) {
            displayManager.removeStatusDisplay(event.getEntity());
        }
    }

    @EventHandler
    public void onLogout(PlayerQuitEvent event) {
        event.getPlayer().getEffects().forEach(SpellEffect::onRemove);
        Set<SpellTrait> toRemove = new HashSet<>(event.getPlayer().getActiveTraits());
        toRemove.stream().map(SpellTrait::getData).forEach(event.getPlayer()::removeTrait);
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

    @EventHandler
    public void onChannelStart(SpellChannelStartEvent event) {
        LivingEntity entity = (LivingEntity) event.getCaster();
        if (!displayManager.hasStatusDisplay(entity)) {
            displayManager.addStatusDisplay(entity, new EntityStatusDisplay(entity));
        }
        displayManager.getStatusDisplay(entity).updateStatusDisplay();
        SpellbookSpell spell = event.getSpell();
        int duration = spell.channelDuration;
        BukkitRunnable channelSound = new BukkitRunnable() {
            @Override
            public void run() {
                if (!entity.isChanneling()) {
                    cancel();
                }
                float pitch = 1.5f - ((float) spell.currentChannelTicks / duration);
                entity.playSound(Sound.sound(org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, Sound.Source.RECORD, 0.66f, pitch));
            }
        };
        if (spell.isMovementInterrupted) {
            channelSound.runTaskTimer(Hecate.getInstance(), 0, 7);
        }
        BukkitRunnable updateChannelProgress = new BukkitRunnable() {
            @Override
            public void run() {
                if (!entity.isChanneling()) {
                    cancel();
                }
                displayManager.getStatusDisplay(entity).updateChannelProgress(spell.currentChannelTicks, duration);
            }
        };
        updateChannelProgress.runTaskTimer(Hecate.getInstance(), 0, 1);
    }

    @EventHandler
    public void onChannelFinish(SpellChannelFinishEvent event) {
        LivingEntity entity = (LivingEntity) event.getCaster();
        if (!displayManager.hasStatusDisplay(entity)) {
            displayManager.addStatusDisplay(entity, new EntityStatusDisplay(entity));
        }
        displayManager.getStatusDisplay(entity).updateStatusDisplay();
        if (event.wasInterrupted()) {
            displayManager.getStatusDisplay(entity).showText(Component.text("Interrupted!", NamedTextColor.RED), 60);
            entity.playSound(Sound.sound(org.bukkit.Sound.ENTITY_VILLAGER_NO, Sound.Source.RECORD, 1, 1));
        }
    }


}
