package de.erethon.hecate.ui;

import de.erethon.hecate.Hecate;
import de.erethon.hecate.data.HCharacter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

public class OverviewMenu implements Listener, InventoryHolder {

    private final Inventory inventory;
    private final HCharacter character;

    private ItemStack traitMenu;
    private ItemStack skillMenu;

    public OverviewMenu(HCharacter character) {
        this.character = character;
        this.inventory = Bukkit.createInventory(this, 9, character.getPlayer().displayName());
        Bukkit.getPluginManager().registerEvents(this, Hecate.getInstance());
        prepareInventory();
        character.getPlayer().openInventory(inventory);
    }

    private void prepareInventory() {
        traitMenu = new ItemStack(Material.GLOWSTONE_DUST);
        ItemMeta meta = traitMenu.getItemMeta();
        //missing method meta.displayName(Component.text("Trait: " + character.getSelectedTraitline().getId(), NamedTextColor.BLUE));
        traitMenu.setItemMeta(meta);
        skillMenu = new ItemStack(Material.ENCHANTED_BOOK);
        meta = skillMenu.getItemMeta();
        meta.displayName(Component.text("Skills", NamedTextColor.BLUE));
        skillMenu.setItemMeta(meta);
        inventory.setItem(2, traitMenu);
        inventory.setItem(6, skillMenu);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getClickedInventory() != inventory) return;
        event.setCancelled(true);
        ItemStack item = event.getCurrentItem();
        if (item == null) return;
        if (item.equals(traitMenu)) {
            //new TraitMenu(character);
            return;
        }
        if (item.equals(skillMenu)) {
            new SkillMenu(character);
            return;
        }
    }


    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
