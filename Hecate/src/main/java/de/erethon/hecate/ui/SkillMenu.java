package de.erethon.hecate.ui;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.hecate.Hecate;
import de.erethon.hecate.casting.HPlayer;
import de.erethon.spellbook.api.SpellData;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class SkillMenu implements Listener, InventoryHolder {

    private Inventory inventory;
    private HPlayer player;
    private List<SpellData> spells = new ArrayList<>();
    private int currentIndex = 0;

    public SkillMenu(HPlayer player) {
        Bukkit.getServer().getSpellbookAPI().getLibrary().getLoaded().forEach((key, value) -> spells.add(value));
        this.player = player;
        inventory = Bukkit.createInventory(this, 54, ChatColor.DARK_RED + "Skills");
        Bukkit.getPluginManager().registerEvents(this, Hecate.getInstance());
        prepareInventory();
        player.getPlayer().openInventory(inventory);
    }

    public void prepareInventory() {
        inventory.setItem(36, new ItemStack(Material.BLUE_STAINED_GLASS_PANE)); // Prev
        for (int i = 37; i < 44; i++) {
            inventory.setItem(i, new ItemStack(Material.GRAY_STAINED_GLASS_PANE)); // Divider
        }
        inventory.setItem(44, new ItemStack(Material.BLUE_STAINED_GLASS_PANE)); // Next
        addAPageOfSpells(0);
    }

    private void addAPageOfSpells(int start) {
        int inventoryIndex = 0;
        for (int i = start; i < start + 36; i++) {
            if (i >= spells.size()) {
                continue;
            }
            SpellData spell = spells.get(i);
            ItemStack itemStack = new ItemStack(Material.BOOK);
            ItemMeta meta = itemStack.getItemMeta();
            meta.displayName(spell.getDisplayName());
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("ID: " + spell.getId()));
            lore.addAll(spell.getDescription());
            meta.lore(lore);
            itemStack.setItemMeta(meta);
            inventory.setItem(inventoryIndex, itemStack);
            inventoryIndex++;
        }
    }

    private void clearPageOfSpells() {
        for (int i = 0; i < 36; i++) {
            inventory.setItem(i, null);
        }
    }

    public void nextPage() {
        currentIndex += 36;
        if (currentIndex > spells.size()) {
            currentIndex = spells.size();
        }
        clearPageOfSpells();
        addAPageOfSpells(currentIndex);
    }

    public void previousPage() {
        currentIndex -= 36;
        if (currentIndex < 0) {
            currentIndex = 0;
        }
        clearPageOfSpells();
        addAPageOfSpells(currentIndex);
    }

    @EventHandler
    private void onClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() != this) {
            return;
        }
        int slot = event.getSlot();
        if (slot >= 45 && slot <= 53) {
            ItemStack itemStack = event.getCursor();
            if (itemStack == null) {
                player.learnSpell(null, slot - 45);
                MessageUtil.sendMessage(player.getPlayer(), "<green>You unlearned the spell at " + (slot - 45) + "!");
                return;
            }
            if (itemStack.getType() == Material.BOOK) {
                ItemMeta meta = itemStack.getItemMeta();
                Component name = meta.displayName();
                for (SpellData spell : spells) {
                    if (spell.getDisplayName().equals(name)) {
                        player.learnSpell(spell, slot - 45);
                        MessageUtil.sendMessage(player.getPlayer(), "<green>You learned " + spell.getName() + " at " + (slot - 45) + "!");
                        return;
                    }
                }
                return;
            }
        }
        if (slot == 36) {
            previousPage();
            event.setCancelled(true);
        } else if (slot == 44) {
            nextPage();
            event.setCancelled(true);
        } else if (slot < 36 && event.getAction() != InventoryAction.PICKUP_ALL && event.getAction() != InventoryAction.PLACE_ALL) { // Spell list
            event.setCancelled(true);
        } else if(slot > 44 && event.getAction() != InventoryAction.PICKUP_ALL && event.getAction() != InventoryAction.PLACE_ALL) { // Lower bar
            event.setCancelled(true);
        }
        else if (slot > 36 && slot < 44) {
            event.setCancelled(true);
        }
    }

    private void onDrag(InventoryDragEvent event) {
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
