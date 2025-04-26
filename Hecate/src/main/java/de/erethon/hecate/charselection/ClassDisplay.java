package de.erethon.hecate.charselection;

import com.destroystokyo.paper.event.player.PlayerUseUnknownEntityEvent;
import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.hecate.Hecate;
import de.erethon.hecate.classes.HClass;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class ClassDisplay extends BaseDisplay implements Listener {

    private final HClass hClass;

    public ClassDisplay(BaseSelection selection, HClass hClass) {
        super(selection);
        this.hClass = hClass;
        loadInventory();
        MessageUtil.broadcastMessage("Initializing class display");
        Bukkit.getPluginManager().registerEvents(this, Hecate.getInstance());
    }

    public HClass getHClass() {
        return hClass;
    }

    @EventHandler
    private void onUnknownEntityInteract(PlayerUseUnknownEntityEvent event) {
        MessageUtil.broadcastMessage("Interacted");
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

    private void loadInventory() {
        // TODO: Hephestus items once someone creates them
        items.set(0, new ItemStack(Items.IRON_SWORD)); // Placeholder item
        selectedSlot = 0;
        switch (hClass.getId()) {
            case "assassin" -> {
                armor.set(0, new ItemStack(Items.LEATHER_HELMET));
                armor.set(1, new ItemStack(Items.LEATHER_CHESTPLATE));
                armor.set(2, new ItemStack(Items.LEATHER_LEGGINGS));
                armor.set(3, new ItemStack(Items.LEATHER_BOOTS));
            }
            case "warrior" -> {
                armor.set(0, new ItemStack(Items.IRON_HELMET));
                armor.set(1, new ItemStack(Items.IRON_CHESTPLATE));
                armor.set(2, new ItemStack(Items.IRON_LEGGINGS));
                armor.set(3, new ItemStack(Items.IRON_BOOTS));
            }
            case "ranger" -> {
                armor.set(0, new ItemStack(Items.CHAINMAIL_HELMET));
                armor.set(1, new ItemStack(Items.CHAINMAIL_CHESTPLATE));
                armor.set(2, new ItemStack(Items.CHAINMAIL_LEGGINGS));
                armor.set(3, new ItemStack(Items.CHAINMAIL_BOOTS));
            }
            case "paladin" -> {
                armor.set(0, new ItemStack(Items.GOLDEN_HELMET));
                armor.set(1, new ItemStack(Items.GOLDEN_CHESTPLATE));
                armor.set(2, new ItemStack(Items.GOLDEN_LEGGINGS));
                armor.set(3, new ItemStack(Items.GOLDEN_BOOTS));
            }
            default -> armor.set(0, new ItemStack(Items.BEACON)); // Placeholder item for errors
        }
    }

}
