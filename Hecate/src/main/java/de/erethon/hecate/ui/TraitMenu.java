package de.erethon.hecate.ui;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.hecate.Hecate;
import de.erethon.hecate.casting.HPlayer;
import de.erethon.hecate.classes.HClass;
import de.erethon.hecate.classes.TraitLineEntry;
import de.erethon.hecate.classes.Traitline;
import io.papermc.paper.adventure.PaperAdventure;
import net.kyori.adventure.text.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.inventory.MenuType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_19_R3.inventory.CraftInventory;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

public class TraitMenu implements Listener, InventoryHolder {

    private static final Hecate plugin = Hecate.getInstance();

    private static final int LOWER_BAR_START = 45;
    private static final int FIRST_TRAITLINE_START = 11;
    private static final int SPACE_BETWEEN_TRAITLINE_ENTRIES = 9;
    private static final int SPACE_BETWEEN_TRAITLINES = 2;
    private static final Material ICON_MATERIAL = Material.BLUE_DYE;

    private Inventory inventory;
    private HPlayer player;
    private HClass hClass;
    private Traitline selectedTraitline;
    private final Traitline[] traitlines = new Traitline[54];
    private final TraitLineEntry[] entries = new TraitLineEntry[54];
    private Component displayName = Component.text("Trait Menu");

    public TraitMenu(HPlayer player) {
        this.player = player;
        this.hClass = player.gethClass();
        if (hClass == null) {
            MessageUtil.sendMessage(player.getPlayer(), "<red>You have no class selected.");
            return;
        }
        Bukkit.getPluginManager().registerEvents(this, plugin);
        prepareInventory();
        player.getPlayer().openInventory(inventory);
    }

    public void prepareInventory() {
        inventory = player.getPlayer().getServer().createInventory(this, 54, displayName);
        if (selectedTraitline != null) {
            MessageUtil.sendMessage(player.getPlayer(), "<green>Selected traitline: " + selectedTraitline.getName());
            int verticalBar = 0;
            while (verticalBar <= 4) {
                setHorizontalBar(FIRST_TRAITLINE_START + (verticalBar * SPACE_BETWEEN_TRAITLINES), verticalBar);
                verticalBar++;
            }
        }

        int traitLineCount = hClass.getTraitlines().size();
        MessageUtil.sendMessage(player.getPlayer(), "<green>Found " + traitLineCount + " traitlines.");
        int traitLineIndex = 0;
        while (traitLineIndex < traitLineCount) {
            Traitline traitline = hClass.getTraitlines().get(traitLineIndex);
            traitlines[traitLineIndex] = traitline;
            ItemStack item = new ItemStack(ICON_MATERIAL);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(traitline.getDisplayName());
            meta.lore(traitline.getDescription());
            item.setItemMeta(meta);
            int slot = LOWER_BAR_START + (traitLineIndex * SPACE_BETWEEN_TRAITLINES);
            inventory.setItem(slot, item);
            traitlines[slot] = traitline;
            traitLineIndex++;
        }
    }

    private ItemStack getItemAt(Traitline traitline, int level, int index, boolean active) {
        TraitLineEntry entry = traitline.getTraitLineEntries(level).get(index);
        ItemStack item = new ItemStack(ICON_MATERIAL);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(entry.data().getDisplayName());
        meta.lore(entry.data().getDescription());
        if (active) {
            meta.setCustomModelData(entry.activeModelData());
        } else {
            meta.setCustomModelData(entry.inactiveModelData());
        }
        item.setItemMeta(meta);
        return item;
    }

    private void setHorizontalBar(int start, int level) {
        int slot = start;
        int row = 0;
        MessageUtil.log("LeveL: " + level + " Start: " + start);
        while (row < 3) {
            MessageUtil.log(selectedTraitline.getId());
            for (TraitLineEntry entry : selectedTraitline.getTraitLineEntries(level)) {
                MessageUtil.log(entry.data().getId());
            }
            inventory.setItem(slot, getItemAt(selectedTraitline, level, row, player.hasTrait(selectedTraitline.getTraitLineEntries(level).get(row).data())));
            slot+= SPACE_BETWEEN_TRAITLINE_ENTRIES;
            row++;
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() != this) {
            return;
        }
        event.setCancelled(true);
        event.setCursor(null);
        int slot = event.getSlot();
        if (traitlines[slot] != null) {
            selectedTraitline = traitlines[slot];
            displayName = selectedTraitline.getDisplayName();
            updateTitle();
            prepareInventory();
            MessageUtil.sendMessage(player.getPlayer(), "<green>Selected traitline: " + selectedTraitline.getName());
        }
    }

    private void updateTitle() {
        CraftPlayer craftPlayer = (CraftPlayer) player.getPlayer();
        ServerPlayer serverPlayer = craftPlayer.getHandle();
        ClientboundOpenScreenPacket packet = new ClientboundOpenScreenPacket(serverPlayer.containerMenu.containerId, MenuType.GENERIC_9x6, PaperAdventure.asVanilla(displayName));
        serverPlayer.connection.send(packet);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
