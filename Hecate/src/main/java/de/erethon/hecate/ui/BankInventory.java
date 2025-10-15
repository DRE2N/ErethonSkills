package de.erethon.hecate.ui;

import de.erethon.hecate.Hecate;
import de.erethon.hecate.data.Bank;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Paginated inventory UI for player banks
 */
public class BankInventory implements InventoryHolder, Listener {

    private final Bank bank;
    private final Player viewer;
    private final boolean isOwner;
    private int currentPage;
    private final Inventory inventory;

    private static final int PREVIOUS_PAGE_SLOT = 45;
    private static final int PAGE_INFO_SLOT = 49;
    private static final int NEXT_PAGE_SLOT = 53;

    public BankInventory(Bank bank, Player viewer, boolean isOwner) {
        this.bank = bank;
        this.viewer = viewer;
        this.isOwner = isOwner;
        this.currentPage = 0;

        this.inventory = Bukkit.createInventory(this, 54, Component.text("Bank", NamedTextColor.GOLD));

        for (int i = 45; i < 54; i++) {
            ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta meta = filler.getItemMeta();
            meta.displayName(Component.empty());
            filler.setItemMeta(meta);
            inventory.setItem(i, filler);
        }

        Bukkit.getPluginManager().registerEvents(this, Hecate.getInstance());
        loadPage(0);
        viewer.openInventory(inventory);
        viewer.playSound(viewer, Sound.BLOCK_CHEST_OPEN, SoundCategory.RECORDS, 1.0f, 1.0f);
        viewer.playSound(viewer, Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.RECORDS, 1.0f, 1.0f);
    }

    private void loadPage(int page) {
        if (page < 0 || page >= bank.getUnlockedPages()) {
            return;
        }

        this.currentPage = page;

        ItemStack[] pageContents = bank.getPageContents(page);
        for (int i = 0; i < 45; i++) { // Slots 0-44 are for bank items
            if (i < pageContents.length) {
                inventory.setItem(i, pageContents[i]);
            } else {
                inventory.setItem(i, null);
            }
        }
        updateNavigationItems();
    }

    private void switchPage(int page) {
        if (page < 0 || page >= bank.getUnlockedPages()) {
            return;
        }

        saveCurrentPage();
        loadPage(page);
    }

    private void updateNavigationItems() {
        // Filler items
        for (int i = 45; i < 54; i++) {
            ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta meta = filler.getItemMeta();
            meta.displayName(Component.empty());
            filler.setItemMeta(meta);
            inventory.setItem(i, filler);
        }

        // Previous page button
        if (currentPage > 0) {
            ItemStack prevPage = new ItemStack(Material.ARROW);
            ItemMeta meta = prevPage.getItemMeta();
            meta.displayName(Component.text("Previous Page", NamedTextColor.YELLOW));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Page " + currentPage + "/" + bank.getUnlockedPages(), NamedTextColor.GRAY));
            meta.lore(lore);
            prevPage.setItemMeta(meta);
            inventory.setItem(PREVIOUS_PAGE_SLOT, prevPage);
        }

        // Page info
        ItemStack pageInfo = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = pageInfo.getItemMeta();
        infoMeta.displayName(Component.text("Page " + (currentPage + 1) + "/" + bank.getUnlockedPages(), NamedTextColor.GOLD));
        List<Component> infoLore = new ArrayList<>();
        infoLore.add(Component.text("Total slots: " + bank.getTotalSlots(), NamedTextColor.GRAY));
        if (bank.getUnlockedPages() < Bank.MAX_PAGES) {
            infoLore.add(Component.text("Can unlock " + (Bank.MAX_PAGES - bank.getUnlockedPages()) + " more pages", NamedTextColor.GREEN));
        } else {
            infoLore.add(Component.text("Max pages unlocked!", NamedTextColor.GREEN));
        }
        infoMeta.lore(infoLore);
        pageInfo.setItemMeta(infoMeta);
        inventory.setItem(PAGE_INFO_SLOT, pageInfo);

        // Next page button
        if (currentPage < bank.getUnlockedPages() - 1) {
            ItemStack nextPage = new ItemStack(Material.ARROW);
            ItemMeta meta = nextPage.getItemMeta();
            meta.displayName(Component.text("Next Page", NamedTextColor.YELLOW));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Page " + (currentPage + 2) + "/" + bank.getUnlockedPages(), NamedTextColor.GRAY));
            meta.lore(lore);
            nextPage.setItemMeta(meta);
            inventory.setItem(NEXT_PAGE_SLOT, nextPage);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() != this) {
            return;
        }

        int slot = event.getRawSlot();

        if (slot >= 45 && slot < 54) {
            event.setCancelled(true);

            if (slot == PREVIOUS_PAGE_SLOT && currentPage > 0) {
                switchPage(currentPage - 1);
            } else if (slot == NEXT_PAGE_SLOT && currentPage < bank.getUnlockedPages() - 1) {
                switchPage(currentPage + 1);
            }
            return;
        }
        if (bank.getOwner().getPlayerId().equals(viewer.getUniqueId()) || isOwner) {
            // Save to ensure data consistency if the owner is the viewer
            Hecate.getInstance().getDatabaseManager().savePlayerData(bank.getOwner());
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() != this) {
            return;
        }
        for (int slot : event.getRawSlots()) {
            if (slot >= 45 && slot < 54) {
                event.setCancelled(true);
                return;
            }
        }
        if (bank.getOwner().getPlayerId().equals(viewer.getUniqueId()) || isOwner) {
            // Save to ensure data consistency if the owner is the viewer
            Hecate.getInstance().getDatabaseManager().savePlayerData(bank.getOwner());
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() != this) {
            return;
        }

        saveCurrentPage();
        Hecate.getInstance().getDatabaseManager().saveBankData(
            bank.getOwner().getPlayerId(),
            bank.serialize(),
            bank.getUnlockedPages()
        ).thenRun(() -> {
            Hecate.log("Saved bank for player " + bank.getOwner().getPlayerId());
        });
        if (bank.getOwner().getPlayerId().equals(viewer.getUniqueId()) || isOwner) {
            // Save to ensure data consistency if the owner is the viewer
            Hecate.getInstance().getDatabaseManager().savePlayerData(bank.getOwner());
        }


        InventoryClickEvent.getHandlerList().unregister(this);
        InventoryCloseEvent.getHandlerList().unregister(this);
        InventoryDragEvent.getHandlerList().unregister(this);
    }

    private void saveCurrentPage() {
        ItemStack[] pageContents = new ItemStack[Bank.SLOTS_PER_PAGE];
        for (int i = 0; i < Bank.SLOTS_PER_PAGE; i++) {
            pageContents[i] = inventory.getItem(i);
        }
        bank.setPageContents(currentPage, pageContents);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

}
