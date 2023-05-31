package de.erethon.hecate.listeners;

import de.erethon.hecate.Hecate;
import de.erethon.hecate.casting.HPlayer;
import de.erethon.hecate.casting.SpecialActionKey;
import de.erethon.hecate.events.CombatModeReason;
import de.erethon.hecate.ui.EntityStatusDisplayManager;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.ranger.pet.RangerPet;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerCastListener implements Listener {

    private ConcurrentHashMap<TextDisplay, Long> displays = new ConcurrentHashMap<>();

    BukkitRunnable remover = new BukkitRunnable() {
        @Override
        public void run() {
            for (Map.Entry<TextDisplay, Long> entry : displays.entrySet()) {
                if (entry.getValue() + 1500 < System.currentTimeMillis()) {
                    TextDisplay display = entry.getKey();
                    Entity vehicle = display.getVehicle();
                    display.remove();
                    displays.remove(entry.getKey());
                    if (vehicle != null) { // Update other displays if the entity is still there
                        updateTransforms(vehicle);
                    }
                }
            }
        }
    };

    public PlayerCastListener() {
        remover.runTaskTimer(Hecate.getInstance(), 0, 20);
    }

    @EventHandler
    public void onModeSwitch(PlayerSwapHandItemsEvent event) {
        HPlayer hPlayer = Hecate.getInstance().getHPlayerCache().getByPlayer(event.getPlayer());
        // The OffHandItem is the item that WOULD BE in the offhand if the event is not cancelled. Thanks Spigot for great method naming!
        if ((event.getOffHandItem() != null && event.getOffHandItem().getType() == Material.STICK || hPlayer.isInCastmode())) {
            event.setCancelled(true);
            hPlayer.switchMode(CombatModeReason.HOTKEY);
        }
    }


    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player && event.getEntity() instanceof LivingEntity entity) {
            addDisplayDamage(player, entity, event.getDamage());
        }
        // TODO: Move event in Papyrus, currently showing damage without penetration
    }

    private void addDisplayDamage(Player player, LivingEntity entity, double damage) {
        double rounded = Math.round(damage * 100.0) / 100.0;
        TextDisplay display = entity.getWorld().spawn(entity.getLocation().add(0, 0,0), TextDisplay.class, textDisplay -> {
            for (Player p : textDisplay.getTrackedPlayers()) { // TODO: Dmg numbers for party member would be cool
                p.hideEntity(Hecate.getInstance(), textDisplay);
            }
            player.showEntity(Hecate.getInstance(), textDisplay);
            entity.addPassenger(textDisplay);
            textDisplay.setBillboard(Display.Billboard.VERTICAL);
            textDisplay.setBackgroundColor(Color.fromARGB(0, 1,1,1));
            textDisplay.text(Component.text("-" + rounded + "❤").color(NamedTextColor.RED));
        });
        displays.put(display, System.currentTimeMillis());
        updateTransforms(entity);
    }

    private void updateTransforms(Entity entity) {
        Map<Display, Long> activeDisplays = new HashMap<>();
        for (org.bukkit.entity.Entity passenger : entity.getPassengers()) {
            if (passenger instanceof TextDisplay display
                    && !(passenger.getPersistentDataContainer().has(EntityStatusDisplayManager.ENTITY_STATUS_KEY, PersistentDataType.BYTE)
                    || passenger.getPersistentDataContainer().has(RangerPet.PET_STATUS_KEY, PersistentDataType.BYTE))) {
                activeDisplays.put(display, displays.get(display));
            }
        }
        List<Display> sorted = activeDisplays.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).map(Map.Entry::getKey).toList();
        float offset = 0.2f;
        for (Display display : sorted) {
            float sideOffset = (float) (entity.getWidth() / 2 + 0.4);
            display.setTransformation(new Transformation(new Vector3f(sideOffset, offset, 0), new AxisAngle4f(0,0,0,0), new Vector3f(1, 1, 1), new AxisAngle4f(0,0,0,0)));
            offset += 0.2f;
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
            if (hPlayer.gethClass() != null && hPlayer.gethClass().getSpecialAction(SpecialActionKey.Q) != null) {
                hPlayer.gethClass().getSpecialAction(SpecialActionKey.Q).queue(event.getPlayer());
            }
        }
    }

    @EventHandler
    public void onRightclick(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            castRightclickAction(event.getPlayer());
        }
    }

    @EventHandler
    public void onRightClickOnEntity(PlayerInteractEntityEvent event) {
        castRightclickAction(event.getPlayer());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // Fixes for spell effects after server crash
        event.getPlayer().setInvisible(false);
        event.getPlayer().setWalkSpeed(0.2f);
    }

    private void castRightclickAction(Player player) {
        HPlayer hPlayer = Hecate.getInstance().getHPlayerCache().getByPlayer(player);
        if (!hPlayer.isInCastmode()) return;
        if (hPlayer.gethClass() != null && hPlayer.gethClass().getSpecialAction(SpecialActionKey.RIGHT_CLICK) != null) {
            hPlayer.gethClass().getSpecialAction(SpecialActionKey.RIGHT_CLICK).queue(player);
        }
    }

}
