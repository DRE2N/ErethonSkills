package de.erethon.hecate.ui;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.hecate.Hecate;
import de.erethon.hecate.casting.HCharacter;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class SkillMenu implements Listener, InventoryHolder {

    private final NamespacedKey key = new NamespacedKey(Hecate.getInstance(), "skillmenu");
    private final Inventory inventory;
    private final HCharacter player;
    private final List<SpellData> spells = new ArrayList<>();
    private int currentIndex = 0;

    public SkillMenu(HCharacter player) {
        this.player = player;
        inventory = Bukkit.createInventory(this, 54, ChatColor.DARK_RED + "Skills");
        if (player.gethClass() == null || player.getSelectedTraitline() == null) {
            MessageUtil.sendMessage(player.getPlayer(), "<red>You have no class/traitline selected.");
            return;
        }
        this.spells.addAll(player.getSelectedTraitline().getSpells());
        Bukkit.getPluginManager().registerEvents(this, Hecate.getInstance());
        prepareInventory();
        player.getPlayer().openInventory(inventory);
    }

    public void prepareInventory() {
        inventory.setItem(45, new ItemStack(Material.BLUE_STAINED_GLASS_PANE)); // Prev
        for (int i = 46; i < 53; i++) {
            inventory.setItem(i, new ItemStack(Material.GRAY_STAINED_GLASS_PANE)); // Divider
        }
        inventory.setItem(53, new ItemStack(Material.BLUE_STAINED_GLASS_PANE)); // Next
        addAPageOfSpells(0);
    }

    private void addAPageOfSpells(int start) {
        int inventoryIndex = 0;
        for (int i = start; i < start + 45; i++) {
            if (i >= spells.size()) {
                continue;
            }
            SpellData spell = spells.get(i);
            inventory.setItem(inventoryIndex, itemFromSpellData(spell));
            inventoryIndex++;
        }
    }

    private void clearPageOfSpells() {
        for (int i = 0; i < 45; i++) {
            inventory.setItem(i, null);
        }
    }

    public void nextPage() {
        currentIndex += 45;
        if (currentIndex > spells.size()) {
            currentIndex = spells.size();
        }
        clearPageOfSpells();
        addAPageOfSpells(currentIndex);
    }

    public void previousPage() {
        currentIndex -= 43;
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
        event.setCancelled(true);
        if (event.getClick() == ClickType.NUMBER_KEY) {
            int slot = event.getHotbarButton();
            ItemStack clicked = event.getCurrentItem();
            ItemMeta meta = clicked.getItemMeta();
            String id = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
            SpellData data = Spellbook.getSpellData(id);
            if (data == null) {
                MessageUtil.sendMessage(player.getPlayer(), "<red>Spell not found!");
                return;
            }
            player.learnSpell(data, slot);
            MessageUtil.sendMessage(player.getPlayer(), "<green>You learned " + data.getId() + " at " + slot + "!");
        }
        int slot = event.getSlot();
        /*
        if (slot >= 45 && slot <= 53) {
            ItemStack itemStack = event.getCursor();
            if (itemStack == null) {
                player.learnSpell(null, slot - 45);
                MessageUtil.sendMessage(player.getPlayer(), "<green>You unlearned the spell at " + (slot - 45) + "!");
                return;
            }
            if (itemStack.getType() == Material.BOOK) {
                ItemMeta meta = itemStack.getItemMeta();
                String id = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
                for (SpellData spell : spells) {
                    if (spell.getId().equals(id)) {
                        player.learnSpell(spell, slot - 45);
                        MessageUtil.sendMessage(player.getPlayer(), "<green>You learned " + spell.getId() + " at " + (slot - 45) + "!");
                        return;
                    }
                }
                return;
            }
        }*/
        if (slot == 45) {
            previousPage();
            event.setCancelled(true);
        } else if (slot == 53) {
            nextPage();
            event.setCancelled(true);
        } else if (slot > 44 && slot < 54) {
            event.setCancelled(true);
        }
    }

    private ItemStack itemFromSpellData(SpellData data) {
        ItemStack itemStack = new ItemStack(Material.BOOK);
        ItemMeta meta = itemStack.getItemMeta();
        meta.displayName(Component.text(data.getId(), NamedTextColor.RED));
        List<Component> withPlaceholders = new ArrayList<>();
        data.getDescription().forEach(component -> withPlaceholders.add(Spellbook.replacePlaceholders(component, player.getPlayer(), data, true, 1).decoration(TextDecoration.ITALIC, false)));
        meta.lore(new ArrayList<>(withPlaceholders));
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, data.getId());
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
