package de.erethon.hecate.listeners;

import de.erethon.hecate.Hecate;
import de.erethon.hecate.casting.HPlayer;
import de.erethon.spellbook.SpellData;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

public class PlayerCastListener implements Listener {

    @EventHandler
    public void onModeSwitch(PlayerSwapHandItemsEvent event) {
        HPlayer hPlayer = Hecate.getInstance().getHPlayerCache().getByPlayer(event.getPlayer());
        // The OffHandItem is the item that WOULD BE in the offhand if the event is not cancelled. Thanks Spigot for great method naming!
        if ((event.getOffHandItem() != null && event.getOffHandItem().getItemMeta() != null && event.getOffHandItem().getItemMeta().getDisplayName().contains("Spell")) || hPlayer.isInCastmode()) {
            event.setCancelled(true);
            hPlayer.switchMode();
        }
    }

    @EventHandler
    public void onItemSwitch(PlayerItemHeldEvent event) {
        HPlayer hPlayer = Hecate.getInstance().getHPlayerCache().getByPlayer(event.getPlayer());
        if (!hPlayer.isInCastmode()) {
            return;
        }
        event.setCancelled(true);
        SpellData spell = hPlayer.getSpellAt(event.getNewSlot());
        if (spell != null && hPlayer.canCast(spell)) {
            hPlayer.getSpellAt(event.getNewSlot()).queue(hPlayer);
            hPlayer.update();
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        HPlayer hPlayer = Hecate.getInstance().getHPlayerCache().getByPlayer((OfflinePlayer) event.getWhoClicked());
        if (hPlayer.isInCastmode()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        HPlayer hPlayer = Hecate.getInstance().getHPlayerCache().getByPlayer(event.getPlayer());
        if (hPlayer.isInCastmode()) {
            event.setCancelled(true);
        }
    }

}
