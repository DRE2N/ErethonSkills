package de.erethon.hecate.listeners;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.hecate.Hecate;
import de.erethon.hecate.casting.SpecialActionKey;
import de.erethon.hecate.charselection.CharacterSelection;
import de.erethon.hecate.data.DatabaseManager;
import de.erethon.hecate.data.HCharacter;
import de.erethon.hecate.data.HPlayer;
import de.erethon.hecate.events.CombatModeReason;
import de.erethon.hecate.ui.DamageColor;
import de.erethon.hecate.ui.EntityStatusDisplayManager;
import de.erethon.hecate.util.ResourcepackHandler;
import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.ranger.beastmaster.pet.RangerPet;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.craftbukkit.entity.CraftPlayer;
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
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
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

    private final Hecate plugin = Hecate.getInstance();
    private ConcurrentHashMap<TextDisplay, Long> displays = new ConcurrentHashMap<>();
    private final DatabaseManager cache =  Hecate.getInstance().getDatabaseManager();

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
        remover.runTaskTimer(plugin, 0, 20);
    }

    @EventHandler
    public void onModeSwitch(PlayerSwapHandItemsEvent event) {
        HCharacter hCharacter = plugin.getDatabaseManager().getCurrentCharacter(event.getPlayer());
        if (hCharacter == null) {
            return;
        }
        // The OffHandItem is the item that WOULD BE in the offhand if the event is not cancelled. Thanks Spigot for great method naming!
        if (event.getOffHandItem().getType() == Material.STICK) {
            hCharacter.switchCastMode(CombatModeReason.HOTKEY, !hCharacter.isInCastMode()); // The ! is important here lol
            event.setCancelled(true);
        }
    }


    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player && event.getEntity() instanceof LivingEntity entity) {
            addDisplayDamage(player, entity, event.getDamage(), event.getDamageType());
        }
    }

    private void addDisplayDamage(Player player, LivingEntity entity, double damage, PDamageType type) {
        double rounded = Math.round(damage * 100.0) / 100.0;
        TextDisplay display = entity.getWorld().spawn(entity.getLocation().add(0, 0,0), TextDisplay.class, textDisplay -> {
            entity.addPassenger(textDisplay);
            textDisplay.setVisibleByDefault(false);
            textDisplay.setBillboard(Display.Billboard.CENTER);
            textDisplay.setBackgroundColor(Color.fromARGB(0, 1,1,1));
            textDisplay.text(Component.text(rounded + "❤").color(DamageColor.getColorForDamageType(type)));
            textDisplay.setInterpolationDuration(10);
            textDisplay.setInterpolationDelay(0);
            textDisplay.setPersistent(false);
        });
        displays.put(display, System.currentTimeMillis());
        player.showEntity(Hecate.getInstance(), display);
        updateTransforms(entity);
    }

    private void updateTransforms(Entity entity) {
        Map<Display, Long> activeDisplays = new HashMap<>();
        for (org.bukkit.entity.Entity passenger : entity.getPassengers()) {
            if (passenger instanceof TextDisplay display
                    && !(passenger.getPersistentDataContainer().has(EntityStatusDisplayManager.ENTITY_STATUS_KEY, PersistentDataType.BYTE)
                    || passenger.getPersistentDataContainer().has(RangerPet.PET_STATUS_KEY, PersistentDataType.BYTE))) {
                if (displays.containsKey(display) && displays.get(display) != null) { // Be safe
                    activeDisplays.put(display, displays.get(display));
                }
            }
        }
        List<Display> sorted = activeDisplays.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).map(Map.Entry::getKey).toList();
        float offset = 0.2f;
        for (Display display : sorted) {
            float sideOffset = (float) (entity.getWidth() / 2 + 0.4);
            display.setTransformation(new Transformation(new Vector3f(sideOffset, offset, 0), new AxisAngle4f(0,0,0,0), new Vector3f(0.66f, 0.66f, 0.66f), new AxisAngle4f(0,0,0,0)));
            offset += 0.2f;
        }


    }

    @EventHandler
    public void onItemSwitch(PlayerItemHeldEvent event) {
        HCharacter hCharacter = cache.getCurrentCharacter(event.getPlayer());
        if (!hCharacter.isInCastMode()) {
            return;
        }
        event.setCancelled(true);
        SpellData spell = hCharacter.getCastingManager().getSpellAtSlot(event.getNewSlot());
        if (spell != null && event.getPlayer().canCast(spell)) {
            spell.queue(event.getPlayer());
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        HCharacter hCharacter = cache.getCurrentCharacter((Player) event.getWhoClicked());
        if (hCharacter == null) {
            return;
        }
        if (hCharacter.isInCastMode()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        HCharacter hCharacter = cache.getCurrentCharacter(event.getPlayer());
        if (hCharacter.isInCastMode()) {
            event.setCancelled(true);
            if (hCharacter.getTraitline() != null && hCharacter.getTraitline().getSpecialAction(SpecialActionKey.Q) != null) {
                hCharacter.getTraitline().getSpecialAction(SpecialActionKey.Q).queue(event.getPlayer());
            }
        }
    }

    @EventHandler
    public void onRightclick(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            castRightclickAction(event.getPlayer());
        }
        if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            castLeftclickAction(event.getPlayer());
        }
    }

    @EventHandler
    public void onRightClickOnEntity(PlayerInteractEntityEvent event) {
        castRightclickAction(event.getPlayer());
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            castLeftclickAction(player);
        }
    }

    @EventHandler
    public void onConnect(AsyncPlayerPreLoginEvent event) {
        if (!Hecate.getInstance().ready) { // Don't allow players to join while Spellbook is still loading
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, Component.text("Server startet...", NamedTextColor.DARK_RED)
                    .append(Component.newline())
                    .append(Component.newline())
                    .append(Component.text("Bitte warte noch einen Moment.", NamedTextColor.GRAY)));
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        MessageUtil.log("Joined " + event.getPlayer().getName() + " (" + event.getPlayer().getUniqueId() + ")");
        // Fixes for spell effects after server crash
        Player player = event.getPlayer();
        player.setInvisible(false);
        player.setWalkSpeed(0.2f);
    }

    @EventHandler
    public void onDisconnect(PlayerQuitEvent event) {
        if (cache.getCurrentCharacter(event.getPlayer()).isInCastMode()) {
            cache.getCurrentCharacter(event.getPlayer()).switchCastMode(CombatModeReason.PLUGIN, false);
        }
        MessageUtil.log("Disconnected " + event.getPlayer().getName() + " (" + event.getPlayer().getUniqueId() + ")");
    }


    private void castRightclickAction(Player player) {
        HCharacter hCharacter = cache.getCurrentCharacter(player);
        if (hCharacter == null || !hCharacter.isInCastMode()) return;
        if (hCharacter.getTraitline() != null && hCharacter.getTraitline().getSpecialAction(SpecialActionKey.RIGHT_CLICK) != null) {
            hCharacter.getTraitline().getSpecialAction(SpecialActionKey.RIGHT_CLICK).queue(player);
        }
    }

    private void castLeftclickAction(Player player) {
        HCharacter hCharacter = cache.getCurrentCharacter(player);
        if (hCharacter == null || !hCharacter.isInCastMode()) return;
        if (hCharacter.getTraitline() != null && hCharacter.getTraitline().getSpecialAction(SpecialActionKey.LEFT_CLICK) != null) {
            hCharacter.getTraitline().getSpecialAction(SpecialActionKey.LEFT_CLICK).queue(player);
        }
    }

    // Lets handle resourcepacks on the proxy in the config phase instead.
    /*private void finishResourcepack(Player player, HPlayer hPlayer) {
        if (hPlayer.isAutoJoinWithLastCharacter() && hPlayer.getSelectedCharacterID() != 0) {
            MessageUtil.log("Auto-joining with last character for " + player.getName() + ".");
            CraftPlayer craftPlayer = (CraftPlayer) player;
            ServerPlayer serverPlayer = craftPlayer.getHandle();
            serverPlayer.server.getPlayerList().switchProfile(serverPlayer, hPlayer.getSelectedCharacterID());
            serverPlayer.setGameMode(serverPlayer.gameMode.getGameModeForPlayer());
            player.removePotionEffect(PotionEffectType.BLINDNESS);
            MessageUtil.sendMessage(player, "<gray>Automatically selected the last character. <br><i>If you want to select a different character, use /h character.");
            return;
        }
        BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                new CharacterSelection(hPlayer);
            }
        };
        runnable.runTaskLater(Hecate.getInstance(), 5);
    }*/
}
