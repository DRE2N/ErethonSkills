package de.erethon.hecate.ui;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.hecate.Hecate;
import de.erethon.hecate.casting.HCharacter;
import de.erethon.hecate.casting.HPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class CharacterSelection implements InventoryHolder, Listener {

    private final HPlayer hPlayer;
    private final Player player;
    private final Inventory inventory;
    private boolean hasSelected = false;

    private ItemStack charCreation = null;

    public CharacterSelection(HPlayer hPlayer) {
        this.inventory = Bukkit.createInventory(this, 9, "Charakter auswählen");
        this.hPlayer = hPlayer;
        this.player = hPlayer.getPlayer();
        Bukkit.getPluginManager().registerEvents(this, Hecate.getInstance());
        prepareInventory();
        player.openInventory(inventory);
        MessageUtil.log(player.getName() + " is now in character selection.");
    }

    public void prepareInventory() {
        for (HCharacter character : hPlayer.getCharacters()) {
            if (character == null) {
                MessageUtil.sendMessage(player, "You have a null character. Please report this to an admin.");
                continue;
            }
            if (character.getCharacterID() == 0) {
                continue;
            }
            ItemStack icon = character.getIcon();
            icon.lore(Arrays.asList(Component.text("ID: " + character.getCharacterID(), NamedTextColor.GRAY), Component.text("Traitline: " + character.getSelectedTraitline().getId())));
            inventory.setItem(character.getCharacterID() - 1, icon);
        }
        charCreation = new ItemStack(Material.GREEN_CONCRETE);
        ItemMeta meta = charCreation.getItemMeta();
        meta.displayName(Component.text("Neuen Charakter erstellen", NamedTextColor.GREEN));
        charCreation.setItemMeta(meta);
        inventory.setItem(8, charCreation);
        MessageUtil.sendMessage(player, "Select a character. Available characters: " + (hPlayer.getCharacters().size() - 1));
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
        int newID = event.getSlot() + 1;
        if (event.getSlot() == 8) {
            newID = hPlayer.getCharacters().size();
            if (newID == 0) {
                newID = 1;
            }
            MessageUtil.sendMessage(player, "Creating new character with ID " + newID);
        }
        CraftPlayer craftPlayer = (CraftPlayer) player;
        ServerPlayer serverPlayer = craftPlayer.getHandle();
        PlayerList playerList = serverPlayer.server.getPlayerList();
        playerList.switchProfile(serverPlayer, newID);
        MessageUtil.log(player.getName() + " has selected character " + newID);
        hasSelected = true;
        player.removePotionEffect(PotionEffectType.BLINDNESS);
        MessageUtil.log(player.getName() + " is no longer in character selection.");
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
