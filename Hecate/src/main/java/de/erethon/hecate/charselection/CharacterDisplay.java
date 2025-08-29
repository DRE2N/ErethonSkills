package de.erethon.hecate.charselection;

import com.destroystokyo.paper.event.player.PlayerUseUnknownEntityEvent;
import de.erethon.hecate.Hecate;
import de.erethon.hecate.data.HCharacter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class CharacterDisplay extends BaseDisplay implements Listener {

    private final HCharacter character;
    private final BaseSelection selection;

    public CharacterDisplay(HCharacter character, BaseSelection selection) {
        super(selection);
        this.character = character;
        this.selection = selection;
        loadInventory();
        Bukkit.getPluginManager().registerEvents(this, Hecate.getInstance());
    }

    @EventHandler
    private void onUnknownEntityInteract(PlayerUseUnknownEntityEvent event) {
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) {
            return; // Only handle main hand interactions
        }
        if (event.getEntityId() != entityId) {
            return;
        }

        if (event.isAttack()) {
            selection.onLeftClick(this);
        } else {
            // Check if player is sneaking for deletion
            if (event.getPlayer().isSneaking() && selection instanceof CharacterSelection characterSelection) {
                characterSelection.onSneakRightClick(this);
            } else {
                selection.onRightClick(this);
            }
        }
    }

    @Override
    public void display(Player player, Location location) {
        super.display(player, location);
    }

    @Override
    public void remove(Player player) {
        super.remove(player);
    }

    public HCharacter getCharacter() {
        return character;
    }

    private void loadInventory() {
        CompoundTag nbt = character.getPlayerDataNBT();
        if (nbt == null) {
            return;
        }
        ListTag inventoryItems = nbt.getList("Inventory").get();
        selectedSlot = character.getPlayerDataNBT().getInt("SelectedItemSlot").get();
        // Copied from Inventory#load
        this.items.clear();
        /* TODO: Mojang changed the way inventories are loaded, this needs to be updated
        for(ItemStackWithSlot itemStackWithSlot : input) {
            if (itemStackWithSlot.isValidInContainer(0)) {
                items.set(itemStackWithSlot.slot(), itemStackWithSlot.stack());
            }
        }*/
    }
}
