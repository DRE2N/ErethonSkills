package de.erethon.hecate.listeners;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.hecate.Hecate;
import de.erethon.hecate.casting.HPlayer;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.utils.NMSUtils;
import io.papermc.paper.adventure.PaperAdventure;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

public class PlayerCastListener implements Listener {

    Set<ArmorStand> armorStandSet = new HashSet<>();

    BukkitRunnable remover = new BukkitRunnable() {
        @Override
        public void run() {
            for (ArmorStand armorStand : armorStandSet) {
                armorStand.remove(Entity.RemovalReason.DISCARDED);
            }
            armorStandSet.clear();
        }
    };

    public PlayerCastListener() {
        remover.runTaskTimer(Hecate.getInstance(), 0, 25);
    }

    @EventHandler
    public void onModeSwitch(PlayerSwapHandItemsEvent event) {
        HPlayer hPlayer = Hecate.getInstance().getHPlayerCache().getByPlayer(event.getPlayer());
        // The OffHandItem is the item that WOULD BE in the offhand if the event is not cancelled. Thanks Spigot for great method naming!
        if ((event.getOffHandItem() != null && event.getOffHandItem().getType() == Material.STICK || hPlayer.isInCastmode())) {
            event.setCancelled(true);
            hPlayer.switchMode();
        }
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            Location location = event.getEntity().getLocation().add(player.getLocation().getDirection().multiply(event.getEntity().getLocation().distance(player.getLocation())).crossProduct(new Vector(1, 0, 1)));
            ArmorStand armorStand = NMSUtils.spawnInvisibleArmorstand(location, true, false, true, true);
            armorStand.getBukkitEntity().setVelocity(new Vector(0, 1, 0));
            armorStand.setCustomName(PaperAdventure.asVanilla(Component.text("-" + event.getDamage() + "❤").color(NamedTextColor.RED)));
            armorStand.setCustomNameVisible(true);
            //armorStandSet.add(armorStand);
        }
    }

    @EventHandler
    public void onItemSwitch(PlayerItemHeldEvent event) {
        HPlayer hPlayer = Hecate.getInstance().getHPlayerCache().getByPlayer(event.getPlayer());
        if (!hPlayer.isInCastmode()) {
            return;
        }
        event.setCancelled(true);
        SpellData spell = hPlayer.getSpellAt(event.getNewSlot());
        if (spell != null && event.getPlayer().canCast(spell)) {
            spell.queue(event.getPlayer());
            hPlayer.update();
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        HPlayer hPlayer = Hecate.getInstance().getHPlayerCache().getByPlayer((OfflinePlayer) event.getWhoClicked());
        if (hPlayer.isInCastmode()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        HPlayer hPlayer = Hecate.getInstance().getHPlayerCache().getByPlayer(event.getPlayer());
        if (hPlayer.isInCastmode()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        event.getPlayer().setInvisible(false); // Fix for spell effect after server crash
    }



}
