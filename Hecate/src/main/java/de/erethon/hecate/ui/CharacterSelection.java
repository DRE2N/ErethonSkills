package de.erethon.hecate.ui;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.hecate.Hecate;
import de.erethon.hecate.casting.HCharacter;
import de.erethon.hecate.casting.HPlayer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

public class CharacterSelection implements InventoryHolder, Listener {

    private final HPlayer hPlayer;
    private final Player player;
    private final Inventory inventory;
    private boolean hasSelected = false;

    public CharacterSelection(HPlayer hPlayer) {
        this.inventory = Bukkit.createInventory(this, 9, "Character Selection");
        this.hPlayer = hPlayer;
        this.player = hPlayer.getPlayer();
        Bukkit.getPluginManager().registerEvents(this, Hecate.getInstance());
        prepareInventory();
        player.openInventory(inventory);
    }

    public void prepareInventory() {
        for (HCharacter character : hPlayer.getCharacters()) {
            if (character.getCharacterID() == 0) {
                continue;
            }
            inventory.setItem(character.getCharacterID() - 1, character.getIcon());
        }
        MessageUtil.sendMessage(player, "Select a character. Available characters: " + hPlayer.getCharacters().size());
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() != this) {
            return;
        }
        event.setCancelled(true);
        if (event.getSlot() < 0 || event.getSlot() > 8 || event.getCurrentItem() == null || event.getCurrentItem().getType().isAir()) {
            return;
        }
        CraftPlayer craftPlayer = (CraftPlayer) player;
        ServerPlayer serverPlayer = craftPlayer.getHandle();
        PlayerList playerList = serverPlayer.server.getPlayerList();
        playerList.switchProfile(serverPlayer, event.getSlot() + 1);
        hasSelected = true;
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() != player) {
            return;
        }
        if (hasSelected) {
            return;
        }
        BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                if (!hasSelected) {
                    player.openInventory(inventory);
                }
            }
        };
        runnable.runTaskLater(Hecate.getInstance(), 1);
    }
}
