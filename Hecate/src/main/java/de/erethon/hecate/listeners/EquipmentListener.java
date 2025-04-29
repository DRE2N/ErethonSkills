package de.erethon.hecate.listeners;

import de.erethon.hecate.Hecate;
import de.erethon.hecate.classes.HClass;
import de.erethon.hecate.data.HCharacter;
import de.erethon.hecate.data.HPlayer;
import de.erethon.hephaestus.Hephaestus;
import de.erethon.hephaestus.items.HItem;
import de.erethon.hephaestus.items.HItemStack;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;

import java.util.Set;

public class EquipmentListener implements Listener {

    private Hecate plugin = Hecate.getInstance();

    // TODO: Handle hotbar right-clicking (or just disable it)
    @EventHandler
    private void onEquip(InventoryClickEvent event) {
        if (event.getSlotType() != InventoryType.SlotType.ARMOR) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        HPlayer hPlayer = plugin.getDatabaseManager().getHPlayer(player);
        HCharacter hCharacter = hPlayer.getSelectedCharacter();
        if (hCharacter == null) {
            return;
        }
        HClass hClass = hCharacter.getHClass();
        if (hClass == null) {
            return;
        }
        if (event.getClick() != ClickType.LEFT) {
            event.setCancelled(true); // Don't care enough to deal with all the edge cases
            player.updateInventory();
            return;
        }
        // Setting an item in the armor slot
        if (event.getCursor().getType() != Material.AIR && (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR)) {
            HItemStack stack = Hephaestus.getStack(event.getCurrentItem());
            if (stack == null) {
                return;
            }
            HItem item = stack.getItem();
            Set<String> itemTags = item.getTags();
            Set<String> classTags = hClass.getArmorTags();
            if (itemTags == null || classTags == null) {
                return;
            }
            // The item needs to have at least one tag that the class has
            boolean hasTag = false;
            for (String tag : itemTags) {
                if (classTags.contains(tag)) {
                    hasTag = true;
                    break;
                }
            }
            if (!hasTag) {
                player.sendRichMessage("<red>You cannot equip this item!");
                event.setCancelled(true);
            }
        }
    }

}
