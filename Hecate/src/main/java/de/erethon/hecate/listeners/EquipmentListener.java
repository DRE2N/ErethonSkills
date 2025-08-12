package de.erethon.hecate.listeners;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import de.erethon.hecate.Hecate;
import de.erethon.hecate.classes.HClass;
import de.erethon.hecate.data.HCharacter;
import de.erethon.hecate.data.HPlayer;
import de.erethon.hecate.items.EquipmentSet;
import de.erethon.hephaestus.Hephaestus;
import de.erethon.hephaestus.items.HItem;
import de.erethon.hephaestus.items.HItemStack;
import de.erethon.spellbook.api.TraitData;
import io.papermc.paper.datacomponent.DataComponentTypes;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MainHand;

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

    // Right-clicking an armor item
    @EventHandler
    private void onInteract(PlayerInteractEvent event) {
        if (!event.hasItem()) {
            return;
        }
        if (!event.getAction().isRightClick()) {
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        HItemStack stack = Hephaestus.getStack(event.getItem());
        if (stack == null) {
            return; // Not an hitem
        }
        Player player = event.getPlayer();
        HPlayer hPlayer = plugin.getDatabaseManager().getHPlayer(player);
        HCharacter hCharacter = hPlayer.getSelectedCharacter();
        if (hCharacter == null) {
            return;
        }
        HClass hClass = hCharacter.getHClass();
        if (hClass == null) {
            return;
        }
        HItem item = stack.getItem();
        Set<String> itemTags = item.getTags();
        Set<String> classTags = hClass.getArmorTags();
        if (itemTags == null || classTags == null || itemTags.isEmpty() || classTags.isEmpty()) {
            return;
        }
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
            event.setUseItemInHand(Event.Result.DENY);
        }
    }

    @EventHandler
    private void onArmorChange(PlayerArmorChangeEvent event) {
        if (event.getNewItem().matchesWithoutData(event.getOldItem(), Set.of(DataComponentTypes.DAMAGE))) {
            return;
        }
        HItemStack stack = Hephaestus.getStack(event.getNewItem());
        if (stack == null) {
            return;
        }
        Player player = event.getPlayer();
        HPlayer hPlayer = plugin.getDatabaseManager().getHPlayer(player);
        HCharacter hCharacter = hPlayer.getSelectedCharacter();
        if (hCharacter == null) {
            return;
        }
        HClass hClass = hCharacter.getHClass();
        if (hClass == null) {
            return;
        }
        HItem item = stack.getItem();
        Set<String> itemTags = item.getTags();
        if (itemTags == null || itemTags.isEmpty()) {
            return;
        }
        itemTags.removeIf(tag -> !tag.startsWith("equipmentset."));
        // we do not support multiple tags for equipment sets
        if (itemTags.size() != 1) {
            return;
        }
        String tag = itemTags.iterator().next();
        int taggedItems = 0;
        for (ItemStack armorItem : player.getEquipment().getArmorContents()) {
            if (armorItem == null || armorItem.getType() == Material.AIR) {
                continue;
            }
            HItemStack armorStack = Hephaestus.getStack(armorItem);
            if (armorStack == null) {
                continue;
            }
            HItem armor = armorStack.getItem();
            Set<String> armorTags = armor.getTags();
            if (armorTags != null && armorTags.contains(tag)) {
                taggedItems++;
            }
        }
        EquipmentSet equipmentSet = plugin.getEquipmentManager().getEquipmentSet(tag);
        if (equipmentSet == null) {
            return;
        }
        Set<TraitData> effects = equipmentSet.effects().get(taggedItems);
        if (effects == null || effects.isEmpty()) {
            return;
        }
        for (TraitData effect : effects) {
            if (player.hasTrait(effect)) {
                continue;
            }
            player.addTrait(effect);
        }
    }

}
