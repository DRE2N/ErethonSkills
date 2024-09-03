package de.erethon.hecate.ui;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.hecate.Hecate;
import de.erethon.hecate.casting.HCharacter;
import de.erethon.hecate.classes.HClass;
import de.erethon.hecate.classes.TraitLineEntry;
import de.erethon.hecate.classes.Traitline;
import io.papermc.paper.adventure.PaperAdventure;
import net.kyori.adventure.text.Component;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class TraitMenu implements Listener, InventoryHolder {

    private static final Hecate plugin = Hecate.getInstance();

    private static final int LOWER_BAR_START = 0;
    private static final int FIRST_TRAITLINE_START = 11;
    private static final int SPACE_BETWEEN_TRAITLINE_ENTRIES = 9;
    private static final int SPACE_BETWEEN_TRAITLINES = 2;
    private static final Material ICON_MATERIAL = Material.BLUE_DYE;

    private Inventory topInventory;
    private Inventory bottomInventory;
    private HCharacter player;
    private HClass hClass;
    private Traitline selectedTraitline;
    private final Traitline[] traitlines = new Traitline[54];
    private final TraitLineEntry[] entries = new TraitLineEntry[54];
    private Component displayName = Component.text("Trait Menu");

    public TraitMenu(HCharacter player) {
        this.player = player;
        this.hClass = player.gethClass();
        if (hClass == null) {
            MessageUtil.sendMessage(player.getPlayer(), "<red>You have no class selected.");
            return;
        }
        if (player.getSelectedTraitline() != null) {
            selectedTraitline = player.getSelectedTraitline();
            displayName = selectedTraitline.getDisplayName();
        }
        if (player.isInCastmode()) {
            MessageUtil.sendMessage(player.getPlayer(), "<red>You can't open the trait menu while in combat.");
            return;
        }
        Bukkit.getPluginManager().registerEvents(this, plugin);
        topInventory = player.getPlayer().getServer().createInventory(this, 9, displayName);
        bottomInventory = player.getPlayer().getInventory();
        player.saveInventory().thenAccept(bool -> {
            if (bool) {
                try {
                    prepareInventory();
                    // Let's go back to the main thread
                    BukkitRunnable runnable = new BukkitRunnable() {
                        @Override
                        public void run() {
                            player.getPlayer().openInventory(topInventory);
                        }
                    };
                    runnable.runTask(plugin);
                } catch (Exception e) {
                    MessageUtil.sendMessage(player.getPlayer(), "&cThere was an error while opening the trait menu. Please report this issue.");
                    e.printStackTrace();
                }
            } else {
                MessageUtil.sendMessage(player.getPlayer(), "&cThere was an error while saving your inventory. Please report this issue.");
            }
        });
    }

    public void prepareInventory()  {
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
            meta.displayName(Component.text(traitline.getId()));
            meta.lore(traitline.getDescription());
            item.setItemMeta(meta);
            int slot = LOWER_BAR_START + (traitLineIndex * SPACE_BETWEEN_TRAITLINES);
            topInventory.setItem(slot, item);
            traitlines[slot] = traitline;
            traitLineIndex++;
        }
    }

    private ItemStack getItemAt(Traitline traitline, int level, int index, boolean active) {
        TraitLineEntry entry = traitline.getTraitLineEntries(level).get(index);
        ItemStack item = new ItemStack(ICON_MATERIAL);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.translatable("spellbook.trait.name." + entry.data().getId(), entry.data().getId()));
        List<Component> lore = new ArrayList<>();
        for (int i = 0; i < entry.data().getDescriptionLineCount(); i++) {
            lore.add(Component.translatable("spellbook.trait.description." + entry.data().getId() + "." + i, ""));
        }
        meta.lore(lore);
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
            bottomInventory.setItem(slot, getItemAt(selectedTraitline, level, row, player.hasTrait(entry.data())));
            entries[slot] = entry;
            slot+= SPACE_BETWEEN_TRAITLINE_ENTRIES;
            row++;
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() != this || event.getView() != player.getPlayer().getOpenInventory()) {
            return;
        }
        event.setCancelled(true);
        event.setCursor(null);
        int slot = event.getSlot();
        if (slot < 0 || slot > 54) {
            return;
        }
        if (traitlines[slot] != null) {
            selectedTraitline = traitlines[slot];
            player.setSelectedTraitline(selectedTraitline);
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

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() != this) {
            return;
        }
        player.loadInventory().thenAccept(bool -> {
            if (!bool) {
                // At least tell the player that we fucked up
                MessageUtil.sendMessage(player.getPlayer(), "<red>There was an error while loading your inventory. Please report this issue.");
            }
        });
    }

    private void updateTitle() {
        CraftPlayer craftPlayer = (CraftPlayer) player.getPlayer();
        ServerPlayer serverPlayer = craftPlayer.getHandle();
        ClientboundOpenScreenPacket packet = new ClientboundOpenScreenPacket(serverPlayer.containerMenu.containerId, MenuType.GENERIC_9x1, PaperAdventure.asVanilla(displayName));
        serverPlayer.connection.send(packet);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return topInventory;
    }
}
