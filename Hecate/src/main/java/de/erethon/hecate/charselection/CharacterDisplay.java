package de.erethon.hecate.charselection;

import com.destroystokyo.paper.event.player.PlayerUseUnknownEntityEvent;
import de.erethon.aether.Aether;
import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.hecate.Hecate;
import de.erethon.hecate.data.HCharacter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
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
            return;
        }
        if (!event.isAttack()) {
            selection.onRightClick(this);
            return;
        }
        if (event.getEntityId() == entityId) {
            selection.onLeftClick(this);
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
        for (int i = 0; i < inventoryItems.size(); i++) {
            CompoundTag compound = inventoryItems.getCompound(i).get();
            int slot = compound.getByte("Slot").get() & 255;
            ItemStack itemStack = ItemStack.parse(MinecraftServer.getServer().registryAccess(), compound).orElse(ItemStack.EMPTY);
            if (slot < items.size()) {
                items.set(slot, itemStack);
            } else if (slot >= 100 && slot < armor.size() + 100) {
                armor.set(slot - 100, itemStack);
            } else if (slot >= 150 && slot < offhand.size() + 150) {
                offhand.set(slot - 150, itemStack);
            }
        }
    }
}
