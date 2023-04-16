package de.erethon.hecate.ui;

import de.erethon.hecate.casting.HPlayer;
import de.erethon.hecate.classes.HClass;
import de.erethon.hecate.classes.Traitline;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public class TraitMenu implements Listener, InventoryHolder {

    private Inventory inventory;
    private HPlayer player;
    private HClass hClass;
    private Traitline selectedTraitline;

    public TraitMenu(HPlayer player) {
        this.player = player;
        inventory = player.getPlayer().getServer().createInventory(this, 54, "Traits");
        prepareInventory();
        player.getPlayer().openInventory(inventory);
    }

    public void prepareInventory() {
        // TODO
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() != this) {
            return;
        }
        event.setCancelled(true);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
