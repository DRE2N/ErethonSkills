package de.erethon.spellbook.spells.warrior.swordstorm;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookSpell;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.BlocksAttacks;
import io.papermc.paper.datacomponent.item.blocksattacks.DamageReduction;
import io.papermc.paper.event.player.PlayerStopUsingItemEvent;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.Vector;
import java.util.List;

public class Blocking extends SwordstormBaseSpell implements Listener {

    private Vector horizontalDirection() {
        Vector direction = caster.getLocation().getDirection().setY(0);
        return direction.lengthSquared() < 0.000001 ? new Vector(0, 0, 1) : direction.normalize();
    }

    // RMB: Block with your sword, reducing incoming damage but also reducing your movement speed.

    private final double blockDamageMultiplier = data.getDouble("blockDamageMultiplier", 0.5);
    private final double movementSpeedReduction = data.getDouble("movementSpeedReduction", -0.05);

    /** The client needs this component on the sword before its first right-click. */
    public static BlocksAttacks nativeSwordBlock() {
        return BlocksAttacks.blocksAttacks()
                .damageReductions(List.of(DamageReduction.damageReduction()
                        .horizontalBlockingAngle(65.0f)
                        .factor(0.5f)
                        .build()))
                .build();
    }

    private final AttributeModifier movementSpeedModifier = new AttributeModifier(NamespacedKey.fromString("spellbook:blocking"), movementSpeedReduction, AttributeModifier.Operation.ADD_NUMBER);
    private boolean currentlyBlocking = false;
    private Vector blockDirection = new Vector(0, 0, 1);
    private ItemStack blockingItem;
    private boolean blockingItemHadBlocksAttacks;
    private int blockingSlot = -1;
    private boolean ended;

    public Blocking(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        tickInterval = 0;
        keepAliveTicks = -1; // Keep the spell alive until manually stopped
    }

    @Override
    protected boolean onPrecast() {
        for (SpellbookSpell spell : caster.getActiveSpells()) { // Prevent double casts
            if (spell instanceof Blocking blocking && !blocking.ended) {
                return false;
            }
        }
        return super.onPrecast();
    }

    @Override
    public boolean onCast() {
        if (caster.getEquipment() == null) {
            return false;
        }
        ItemStack mainHand = caster.getEquipment().getItemInMainHand();
        if (mainHand.getType().isAir()) {
            return false;
        }
        if (!super.onCast()) return false;
        blockingItem = mainHand;
        if (caster instanceof Player player) {
            blockingSlot = player.getInventory().getHeldItemSlot();
        }
        blockingItemHadBlocksAttacks = mainHand.hasData(DataComponentTypes.BLOCKS_ATTACKS);
        // Papyrus custom damage uses onDamage below; vanilla damage uses this item component.
        // Both paths must produce the same directional, partial sword block.
        BlocksAttacks blocksAttacks = nativeSwordBlock();
        mainHand.setData(DataComponentTypes.BLOCKS_ATTACKS, blocksAttacks);
        if (caster instanceof Player player) {
            player.getInventory().setItemInMainHand(mainHand);
        }
        Bukkit.getPluginManager().registerEvents(this, Spellbook.getInstance().getImplementer());
        startBlocking();
        return true;
    }

    @Override
    public double onDamage(LivingEntity attacker, double damage, PDamageType type) {
        if (currentlyBlocking && attacker != null) {
            Vector incoming = attacker.getLocation().toVector().subtract(caster.getLocation().toVector()).setY(0);
            if (incoming.lengthSquared() < 0.001 ||
                    blockDirection.dot(incoming.normalize()) < 0.45) return super.onDamage(attacker, damage, type);
            damage *= blockDamageMultiplier;
        }
        return super.onDamage(attacker, damage, type);
    }

    @Override
    protected void onTick() {
        super.onTick();
        if (!(caster instanceof Player player) || !player.isOnline() || player.isDead()
                || player.getInventory().getHeldItemSlot() != blockingSlot
                || !player.getInventory().getItemInMainHand().hasData(DataComponentTypes.BLOCKS_ATTACKS)) {
            keepAliveTicks = currentTicks;
            return;
        }
        if (currentlyBlocking) {
            blockDirection = horizontalDirection();
        }
    }

    // TODO: This might need some Papyrus changes for blocks that have interactions (e.g. our note blocks)
    @EventHandler
    private void onUse(PlayerInteractEvent event) {
        if (event.getPlayer() != caster || event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        ItemStack item = event.getPlayer().getEquipment().getItemInMainHand();
        if (event.getPlayer().getInventory().getHeldItemSlot() == blockingSlot
                && item.hasData(DataComponentTypes.BLOCKS_ATTACKS) && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            startBlocking();
            event.setUseInteractedBlock(Event.Result.DENY);
            return;
        }
        if (event.getPlayer().getInventory().getHeldItemSlot() == blockingSlot
                && item.hasData(DataComponentTypes.BLOCKS_ATTACKS) && event.getAction() == Action.RIGHT_CLICK_AIR) {
            startBlocking();
            return;
        }
    }

    @EventHandler
    private void onStopUse(PlayerStopUsingItemEvent event) {
        if (event.getPlayer() != caster || !currentlyBlocking) {
            return;
        }
        endNow();
    }

    @EventHandler
    private void onItemHeld(PlayerItemHeldEvent event) {
        if (event.getPlayer() == caster) {
            endNow();
        }
    }

    @EventHandler
    private void onSwapHands(PlayerSwapHandItemsEvent event) {
        if (event.getPlayer() == caster) {
            endNow();
        }
    }

    @EventHandler
    private void onQuit(PlayerQuitEvent event) {
        if (event.getPlayer() == caster) {
            endNow();
        }
    }

    @EventHandler
    private void onDeath(PlayerDeathEvent event) {
        if (event.getPlayer() == caster) {
            endNow();
        }
    }

    private void startBlocking() {
        if (currentlyBlocking) {
            return;
        }
        currentlyBlocking = true;
        blockDirection = horizontalDirection();
        caster.getAttribute(Attribute.MOVEMENT_SPEED).removeModifier(movementSpeedModifier);
        caster.getAttribute(Attribute.MOVEMENT_SPEED).addTransientModifier(movementSpeedModifier);
    }

    private void stopBlocking() {
        currentlyBlocking = false;
        caster.getAttribute(Attribute.MOVEMENT_SPEED).removeModifier(movementSpeedModifier);
    }

    private void endNow() {
        if (ended) return;
        cleanup();
        caster.getActiveSpells().remove(this);
    }

    @Override
    protected void cleanup() {
        if (ended) return;
        ended = true;
        stopBlocking();
        if (caster instanceof Player player && blockingSlot >= 0 && !blockingItemHadBlocksAttacks) {
            ItemStack item = player.getInventory().getItem(blockingSlot);
            if (item != null && blockingItem != null && item.getType() == blockingItem.getType()
                    && item.hasData(DataComponentTypes.BLOCKS_ATTACKS)) {
                item.unsetData(DataComponentTypes.BLOCKS_ATTACKS);
                player.getInventory().setItem(blockingSlot, item);
            }
        }
        HandlerList.unregisterAll(this);
        super.cleanup();
    }
}
