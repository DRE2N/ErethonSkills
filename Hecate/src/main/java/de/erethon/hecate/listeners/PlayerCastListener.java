package de.erethon.hecate.listeners;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.hecate.Hecate;
import de.erethon.hecate.casting.HCharacter;
import de.erethon.hecate.casting.HPlayer;
import de.erethon.hecate.casting.HPlayerCache;
import de.erethon.hecate.casting.SpecialActionKey;
import de.erethon.hecate.events.CombatModeReason;
import de.erethon.hecate.ui.CharacterSelection;
import de.erethon.hecate.ui.DamageColor;
import de.erethon.hecate.ui.EntityStatusDisplayManager;
import de.erethon.papyrus.DamageType;
import de.erethon.papyrus.PlayerSwitchProfileEvent;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.ranger.pet.RangerPet;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftPlayer;
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
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import javax.naming.Name;
import java.io.File;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerCastListener implements Listener {

    private ConcurrentHashMap<TextDisplay, Long> displays = new ConcurrentHashMap<>();
    private final HPlayerCache cache =  Hecate.getInstance().getHPlayerCache();

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
        HCharacter hCharacter = cache.getCharacter(event.getPlayer());
        // The OffHandItem is the item that WOULD BE in the offhand if the event is not cancelled. Thanks Spigot for great method naming!
        if ((event.getOffHandItem() != null && event.getOffHandItem().getType() == Material.STICK || hCharacter.isInCastmode())) {
            event.setCancelled(true);
            if (!hCharacter.isInCastmode()) {
                hCharacter.saveInventory().thenAccept(bool -> {
                    if (bool) {
                        BukkitRunnable runnable = new BukkitRunnable() {
                            @Override
                            public void run() {
                                hCharacter.switchMode(CombatModeReason.HOTKEY);
                            }
                        };
                        runnable.runTask(Hecate.getInstance());
                    } else {
                        MessageUtil.sendMessage(event.getPlayer(), "&cThere was an error while saving your inventory. Please report this issue.");
                    }
                });
                return;
            }
            hCharacter.loadInventory().thenAccept(bool -> {
                if (bool) {
                    BukkitRunnable runnable = new BukkitRunnable() {
                        @Override
                        public void run() {
                            hCharacter.switchMode(CombatModeReason.HOTKEY);
                        }
                    };
                    runnable.runTask(Hecate.getInstance());
                } else {
                    MessageUtil.sendMessage(event.getPlayer(), "&cThere was an error while loading your inventory. Please report this issue.");
                }
            });
        }
    }


    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player && event.getEntity() instanceof LivingEntity entity) {
            addDisplayDamage(player, entity, event.getDamage(), event.getDamageType());
        }
    }

    private void addDisplayDamage(Player player, LivingEntity entity, double damage, DamageType type) {
        double rounded = Math.round(damage * 100.0) / 100.0;
        TextDisplay display = entity.getWorld().spawn(entity.getLocation().add(0, 0,0), TextDisplay.class, textDisplay -> {
            for (Player p : textDisplay.getTrackedPlayers()) { // TODO: Dmg numbers for party member would be cool
                p.hideEntity(Hecate.getInstance(), textDisplay);
            }
            player.showEntity(Hecate.getInstance(), textDisplay);
            entity.addPassenger(textDisplay);
            textDisplay.setBillboard(Display.Billboard.VERTICAL);
            textDisplay.setBackgroundColor(Color.fromARGB(0, 1,1,1));
            textDisplay.text(Component.text("-" + rounded + "❤").color(DamageColor.getColorForDamageType(type)));
            textDisplay.setPersistent(false);
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
        HCharacter hCharacter = cache.getCharacter(event.getPlayer());
        if (!hCharacter.isInCastmode()) {
            return;
        }
        event.setCancelled(true);
        SpellData spell = hCharacter.getSpellAt(event.getNewSlot());
        if (spell != null && event.getPlayer().canCast(spell)) {
            spell.queue(event.getPlayer());
            hCharacter.update();
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        HCharacter hCharacter = cache.getCharacter((Player) event.getWhoClicked());
        if (hCharacter == null) {
            return;
        }
        if (hCharacter.isInCastmode()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        HCharacter hCharacter = cache .getCharacter(event.getPlayer());
        if (hCharacter.isInCastmode()) {
            event.setCancelled(true);
            if (hCharacter.gethClass() != null && hCharacter.gethClass().getSpecialAction(SpecialActionKey.Q) != null) {
                hCharacter.gethClass().getSpecialAction(SpecialActionKey.Q).queue(event.getPlayer());
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
        HPlayer hPlayer = cache.getByPlayer(player);
        if (hPlayer.isAutoJoinWithLastCharacter() && hPlayer.getSelectedCharacterID() != 0) {
            CraftPlayer craftPlayer = (CraftPlayer) player;
            ServerPlayer serverPlayer = craftPlayer.getHandle();
            serverPlayer.server.getPlayerList().switchProfile(serverPlayer, hPlayer.getSelectedCharacterID());
            return;
        }
        BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                new CharacterSelection(hPlayer);
            }
        };
        runnable.runTaskLater(Hecate.getInstance(), 5);
    }

    @EventHandler
    public void onDisconnect(PlayerQuitEvent event) {
        if (cache.getCharacter(event.getPlayer()).isInCastmode()) {
            cache.getCharacter(event.getPlayer()).switchMode(CombatModeReason.PLUGIN);
        }
        cache.getByPlayer(event.getPlayer()).saveUser();
        event.getPlayer().saveData();
        cache.remove(cache.getByPlayer(event.getPlayer()));
        MessageUtil.log("Disconnected " + event.getPlayer().getName() + " (" + event.getPlayer().getUniqueId() + ")");
    }

    @EventHandler
    public void onSwitch(PlayerSwitchProfileEvent event) {
        Player player = event.getPlayer();
        int id = event.getNewProfileID();
        File file = new File(Hecate.getInstance().getDataFolder(), "inventories/" + player.getUniqueId() + "_" + id + ".yml");
        if (!file.exists()) {
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        if (config.getBoolean("active")) {
            HCharacter hCharacter = cache.getCharacter(player);
            hCharacter.loadInventory().thenAccept(bool -> {
                if (!bool) {
                    MessageUtil.sendMessage(player, "&cThere was an error while loading your inventory. Please report this issue.");
                }
            });
        }
    }

    @EventHandler
    public void preCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        HPlayer hPlayer = cache.getByPlayer(player);
        if (hPlayer.getSelectedCharacter() == null && !event.getMessage().equals("/h character")) {
            MessageUtil.sendMessage(player, "&cYou need to select a character first.");
            event.setCancelled(true);
        }
    }

    private void castRightclickAction(Player player) {
        HCharacter hCharacter = cache.getCharacter(player);
        if (!hCharacter.isInCastmode()) return;
        if (hCharacter.gethClass() != null && hCharacter.gethClass().getSpecialAction(SpecialActionKey.RIGHT_CLICK) != null) {
            hCharacter.gethClass().getSpecialAction(SpecialActionKey.RIGHT_CLICK).queue(player);
        }
    }
}
