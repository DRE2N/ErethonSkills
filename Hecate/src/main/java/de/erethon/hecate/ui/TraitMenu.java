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
import org.bukkit.entity.Player;
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
        inventory = player.getPlayer().getServer().createInventory(this, 54, displayName);
        prepareInventory();
        player.getPlayer().openInventory(inventory);
    }

    public void prepareInventory() {
        if (selectedTraitline != null) {
            int horizontalBar = 0;
            while (horizontalBar <= 3) {
                setVerticalBar(FIRST_TRAITLINE_START + (horizontalBar * SPACE_BETWEEN_TRAITLINES), horizontalBar);
                horizontalBar++;
            }
        }

        int traitLineCount = hClass.getTraitlines().size();
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

    private void setVerticalBar(int start, int level) {
        int slot = start;
        int row = 0;
        while (row < 3) {
            if (selectedTraitline.getTraitLineEntries(level) == null) {
                continue;
            }
            TraitLineEntry entry = selectedTraitline.getTraitLineEntries(level).get(row);
            inventory.setItem(slot, getItemAt(selectedTraitline, level, row, player.hasTrait(entry.data())));
            entries[slot] = entry;
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
        }
        if (entries[slot] != null) {
            TraitLineEntry entry = entries[slot];
            Player bukkitPlayer = player.getPlayer();
            if (bukkitPlayer.hasTrait(entry.data())) {
                bukkitPlayer.removeTrait(entry.data());
                MessageUtil.sendMessage(bukkitPlayer, "<red>Unselected trait: " + entry.data().getId());
            } else if (entry.combatOnly()) {
                player.addCombatOnlyTrait(entry.data());
                MessageUtil.sendMessage(bukkitPlayer, "<green>Selected trait: " + entry.data().getId() + "*");
            } else {
                bukkitPlayer.addTrait(entry.data());
                MessageUtil.sendMessage(bukkitPlayer, "<green>Selected trait: " + entry.data().getId());
            }
            prepareInventory();
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
