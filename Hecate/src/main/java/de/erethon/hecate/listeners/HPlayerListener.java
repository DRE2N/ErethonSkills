package de.erethon.hecate.listeners;

import com.destroystokyo.paper.event.player.PlayerPickupExperienceEvent;
import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent;
import com.destroystokyo.paper.event.server.ServerTickStartEvent;
import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.hecate.Hecate;
import de.erethon.hecate.casting.SpecialActionKey;
import de.erethon.hecate.data.DatabaseManager;
import de.erethon.hecate.data.HCharacter;
import de.erethon.hecate.data.HPlayer;
import de.erethon.hecate.events.CombatModeEnterEvent;
import de.erethon.hecate.events.CombatModeReason;
import de.erethon.hecate.progression.LevelUtil;
import de.erethon.hecate.ui.BankInventory;
import de.erethon.hecate.ui.DamageColor;
import de.erethon.papyrus.PDamageType;
import de.erethon.papyrus.combat.CombatDamageBreakdown;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellEffect;
import de.erethon.spellbook.spells.vfx.SkillVfx;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HPlayerListener implements Listener {

    private static final double BASIC_ATTACK_RAY_SIZE = 1.0;
    private static final double DEFAULT_BASIC_ATTACK_RANGE = 3.0;
    private static final int RESISTED_HIT_SOUND_COOLDOWN_TICKS = 4;
    private static final Key DAMAGE_FONT = Key.key("hecate:combat_damage");
    private static final Color DAMAGE_BACKGROUND = Color.fromARGB(130, 18, 17, 23);

    private final Hecate plugin = Hecate.getInstance();
    private final Map<TextDisplay, DamageDisplay> displays = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> lastResistedHitSound = new HashMap<>();
    private final DatabaseManager cache =  Hecate.getInstance().getDatabaseManager();

    BukkitRunnable remover = new BukkitRunnable() {
        @Override
        public void run() {
            long now = System.currentTimeMillis();
            for (Map.Entry<TextDisplay, DamageDisplay> entry : displays.entrySet()) {
                if (entry.getValue().shownAt() + 1500 < now) {
                    TextDisplay display = entry.getKey();
                    Entity vehicle = display.getVehicle();
                    display.remove();
                    displays.remove(entry.getKey());
                    if (vehicle != null) { // Update other displays if the entity is still there
                        updateTransforms(vehicle, entry.getValue().viewer());
                    }
                }
            }
        }
    };

    public HPlayerListener() {
        remover.runTaskTimer(plugin, 0, 5);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity target)) {
            return;
        }
        double damage = event.getDamage();
        CombatDamageBreakdown breakdown = event.getCombatBreakdown();
        if (damage <= 0 && breakdown != null && breakdown.damageAfterCritical() > 0
                && Double.isFinite(breakdown.damageAfterCritical())
                && breakdown.damageAfterResistance() <= 0) {
            Entity attacker = event.getDamageSource().getCausingEntity();
            if (attacker == null) {
                attacker = event.getDamager();
            }
            Location impact = target.getLocation();
            if (attacker instanceof Player player) {
                playResistedHit(player, impact);
            }
            if (target instanceof Player player && !player.equals(attacker)) {
                playResistedHit(player, impact);
            }
            return;
        }
        if (event.getDamager() instanceof Player player) {
            addDisplayDamage(player, target, damage, event.getDamageType());
        }
    }

    private void playResistedHit(Player viewer, Location impact) {
        UUID viewerId = viewer.getUniqueId();
        int tick = Bukkit.getCurrentTick();
        Integer lastTick = lastResistedHitSound.get(viewerId);
        if (lastTick != null && tick - lastTick < RESISTED_HIT_SOUND_COOLDOWN_TICKS) {
            return;
        }
        lastResistedHitSound.put(viewerId, tick);
        viewer.playSound(impact, Sound.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 0.2f, 1.6f);
    }

    private void addDisplayDamage(Player player, LivingEntity entity, double damage, PDamageType type) {
        String rounded = BigDecimal.valueOf(Math.round(damage * 100.0), 2).stripTrailingZeros().toPlainString();
        TextColor color = TextColor.lerp(0.35f, DamageColor.getColorForDamageType(type), NamedTextColor.WHITE);
        Component number = Component.text(rounded, color);
        if (player.hasMetadata(SkillVfx.LOADED_METADATA)) {
            number = number.font(DAMAGE_FONT);
        }
        Component text = number;
        TextDisplay display = entity.getWorld().spawn(entity.getLocation().add(0, 0,0), TextDisplay.class, textDisplay -> {
            entity.addPassenger(textDisplay);
            textDisplay.setVisibleByDefault(false);
            textDisplay.setBillboard(Display.Billboard.CENTER);
            textDisplay.setDefaultBackground(false);
            textDisplay.setBackgroundColor(DAMAGE_BACKGROUND);
            textDisplay.text(text);
            textDisplay.setInterpolationDuration(4);
            textDisplay.setInterpolationDelay(0);
            textDisplay.setPersistent(false);
        });
        UUID viewer = player.getUniqueId();
        displays.put(display, new DamageDisplay(viewer, System.currentTimeMillis()));
        player.showEntity(Hecate.getInstance(), display);
        updateTransforms(entity, viewer);
    }

    private void updateTransforms(Entity entity, UUID viewer) {
        List<Entity> sorted = entity.getPassengers().stream()
                .filter(passenger -> {
                    DamageDisplay damage = displays.get(passenger);
                    return damage != null && damage.viewer().equals(viewer);
                })
                .sorted(Comparator.comparingLong((Entity passenger) -> displays.get(passenger).shownAt())
                        .thenComparingInt(Entity::getEntityId).reversed())
                .toList();
        float sideOffset = (float) (entity.getWidth() / 2 + 0.4);
        for (int index = 0; index < sorted.size(); index++) {
            Display display = (Display) sorted.get(index);
            float scale = index == 0 ? 0.50f : 0.40f;
            display.setTransformation(new Transformation(new Vector3f(sideOffset, 0.18f + index * 0.12f, 0),
                    new AxisAngle4f(), new Vector3f(scale), new AxisAngle4f()));
        }
    }

    private record DamageDisplay(UUID viewer, long shownAt) { }

    @EventHandler
    public void onItemSwitch(PlayerItemHeldEvent event) {
        HCharacter hCharacter = cache.getCurrentCharacter(event.getPlayer());
        if (hCharacter == null || !hCharacter.isInCastMode()) {
            return;
        }
        event.setCancelled(true);
        SpellData spell = hCharacter.getCastingManager().getSpellAtSlot(event.getNewSlot());
        if (spell != null && event.getPlayer().canCast(spell)) {
            spell.queue(event.getPlayer());
            hCharacter.getCastingManager().updateCastingItemName(spell);
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
        if (hCharacter != null && hCharacter.isInCastMode()) {
            event.setCancelled(true);
            if (hCharacter.getTraitline() != null && hCharacter.getTraitline().getSpecialAction(SpecialActionKey.Q) != null) {
                hCharacter.getTraitline().getSpecialAction(SpecialActionKey.Q).queue(event.getPlayer());
            }
        }
    }

    @EventHandler
    public void onRightclick(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.HAND
                && (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            castRightclickAction(event.getPlayer());
        }
        if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            if (!tryBasicAttack(event.getPlayer())) {
                castLeftclickAction(event.getPlayer());
            }
        }
        // Handle bank access
        if (event.getAction().isRightClick() && event.hasBlock() && event.getClickedBlock() != null && event.getClickedBlock().getType() == Material.ENDER_CHEST) {
            Player player = event.getPlayer();
            HPlayer hPlayer = cache.getHPlayer(player);
            Hecate.getInstance().getDatabaseManager().loadBankData(hPlayer.getPlayerId()).thenAccept(bank -> {
                if (bank == null) {
                    MessageUtil.sendMessage(player, "<red>Failed to load bank data.");
                    return;
                }

                Bukkit.getScheduler().runTask(Hecate.getInstance(), () -> {
                    new BankInventory(bank, player, true);
                });
            }).exceptionally(ex -> {
                MessageUtil.sendMessage(player, "<red>Error loading bank: " + ex.getMessage());
                ex.printStackTrace();
                return null;
            });
            event.setUseInteractedBlock(Event.Result.DENY);
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onRightClickOnEntity(PlayerInteractEntityEvent event) {
        castRightclickAction(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player
                && event.getEntity() instanceof LivingEntity target
                && Spellbook.canAttack(player, target)) {
            castLeftclickAction(player);
        }
    }

    @EventHandler
    public void onConnect(AsyncPlayerPreLoginEvent event) {
        if (!Hecate.getInstance().ready) { // Don't allow players to join before Hecate finishes initializing
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, Component.text("Server startet...", NamedTextColor.DARK_RED)
                    .append(Component.newline())
                    .append(Component.newline())
                    .append(Component.text("Bitte warte noch einen Moment.", NamedTextColor.GRAY)));
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // Fixes for spell effects after server crash
        Player player = event.getPlayer();
        player.setInvisible(false);
        player.setWalkSpeed(0.2f);
        player.setHealthScaled(true);
        player.setHealthScale(20.0);
    }

    @EventHandler
    public void onDisconnect(PlayerQuitEvent event) {
        lastResistedHitSound.remove(event.getPlayer().getUniqueId());
        if (cache.getCurrentCharacter(event.getPlayer()).isInCastMode()) {
            cache.getCurrentCharacter(event.getPlayer()).switchCastMode(CombatModeReason.PLUGIN, false);
        }
        // Remove lingering spell effects
        for (SpellEffect effect : event.getPlayer().getEffects()) {
            effect.onRemove();
        }
    }

    @EventHandler
    private void onXpReceive(PlayerPickupExperienceEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    private void onXpDrop(EntitySpawnEvent event) {
        if (event.getEntity() instanceof ExperienceOrb orb) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onRespawn(PlayerPostRespawnEvent event) {
        LevelUtil.displayCharLevel(event.getPlayer());
    }

    @EventHandler
    private void onDeath(PlayerDeathEvent event) {
        Component deathMessage = event.deathMessage();
        event.deathMessage(null);
        if (deathMessage != null) {
            event.getPlayer().sendMessage(deathMessage);
        }

        // Remove lingering spell effects
        for (SpellEffect effect : event.getPlayer().getEffects()) {
            effect.onRemove();
        }
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

    private boolean tryBasicAttack(Player player) {
        if (cache.getCurrentCharacter(player) == null) {
            return false;
        }

        AttributeInstance interactionRange = player.getAttribute(Attribute.ENTITY_INTERACTION_RANGE);
        double range = interactionRange == null ? DEFAULT_BASIC_ATTACK_RANGE : interactionRange.getValue();
        RayTraceResult result = player.getWorld().rayTrace(
                player.getEyeLocation(),
                player.getEyeLocation().getDirection(),
                range,
                FluidCollisionMode.NEVER,
                true,
                BASIC_ATTACK_RAY_SIZE,
                entity -> entity instanceof LivingEntity living
                        && entity != player
                        && Spellbook.canAttack(player, living)
        );

        if (result != null && result.getHitEntity() instanceof LivingEntity target) {
            player.attack(target);
            return true;
        }
        return false;
    }

    // Lets handle resourcepacks on the proxy in the config phase instead.
    /*private void finishResourcepack(Player player, HPlayer hPlayer) {
        if (hPlayer.isAutoJoinWithLastCharacter() && hPlayer.getSelectedCharacterID() != 0) {
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
