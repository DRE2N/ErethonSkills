package de.erethon.spellbook.spells.paladin.guardian;
import de.erethon.spellbook.utils.TargetedParticles;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.aoe.AoE;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.combat.BossControl;
import de.erethon.spellbook.combat.CombatSourceContext;
import de.erethon.spellbook.combat.HealingContext;
import de.erethon.spellbook.api.SpellbookSpell;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.BlocksAttacks;
import io.papermc.paper.event.player.PlayerStopUsingItemEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.List;

public class ShieldOfFaith extends GuardianBaseSpell implements Listener {

    // RMB: The Guardian raises his shield, protecting himself from attacks from the front.
    // For blocked attacks, the Paladin and his allies are healed.

    private final double blockDamageMultiplier = data.getDouble("blockDamageMultiplier", 0.3);
    private final double movementSpeedReduction = data.getDouble("movementSpeedReduction", -0.1);
    private final double healAmount = data.getDouble("healAmount", 8.0);
    private final double healRange = data.getDouble("healRange", 12.0);
    private final double counterRange = data.getDouble("counterRange", 3.2);
    private final double counterDamage = data.getDouble("counterDamage", 12);
    private final int maximumHoldTicks = data.getInt("maximumHoldTicks", 40);
    private final int holyLightDuration = data.getInt("holyLightDuration", 60);

    private final AttributeModifier movementSpeedModifier = new AttributeModifier(
            NamespacedKey.fromString("spellbook:shield_faith"),
            movementSpeedReduction,
            AttributeModifier.Operation.ADD_NUMBER
    );

    private boolean currentlyBlocking = false;
    private int blockingStartedTick;
    private AoE holyGroundAoE;
    private int lastBlockedTick = -100;
    private ItemStack blockingItem;
    private boolean blockingItemHadBlocksAttacks;
    private boolean ended;

    public ShieldOfFaith(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = -1;
    }

    @Override
    protected boolean onPrecast() {
        for (SpellbookSpell spell : caster.getActiveSpells()) {
            if (spell instanceof ShieldOfFaith shield && !shield.ended) {
                return false;
            }
        }
        return super.onPrecast() && hasEnergy(caster, data);
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
        blockingItemHadBlocksAttacks = mainHand.hasData(DataComponentTypes.BLOCKS_ATTACKS);
        BlocksAttacks blocksAttacks = BlocksAttacks.blocksAttacks().build();
        mainHand.setData(DataComponentTypes.BLOCKS_ATTACKS, blocksAttacks);
        if (caster instanceof Player player) {
            player.getInventory().setItemInMainHand(mainHand);
        }

        createHolyGroundAoE();

        Bukkit.getPluginManager().registerEvents(this, Spellbook.getInstance().getImplementer());
        startBlocking();
        Bukkit.getScheduler().runTaskLater(Spellbook.getInstance().getImplementer(), () -> {
            if (!ended) endNow(true);
        }, maximumHoldTicks + 1L);
        return true;
    }

    @Override
    protected void onTick() {
        super.onTick();

        if (currentlyBlocking) {
            if (Bukkit.getCurrentTick() - blockingStartedTick >= maximumHoldTicks) {
                releaseCounter();
                stopBlocking();
                keepAliveTicks = currentTicks;
                return;
            }
            if (caster instanceof Player player && Bukkit.getCurrentTick() - blockingStartedTick > 5
                    && (!player.isOnline() || player.isDead() || !player.isHandRaised())) {
                if (player.isOnline() && !player.isDead()) releaseCounter();
                stopBlocking();
                keepAliveTicks = currentTicks;
                return;
            }
        }
    }

    @Override
    public double onDamage(LivingEntity attacker, double damage, PDamageType type) {
        if (currentlyBlocking && isFrontFacing(attacker)) {
            damage *= blockDamageMultiplier;

            caster.getWorld().playSound(caster.getLocation(), Sound.ITEM_SHIELD_BLOCK, SoundCategory.RECORDS, 1, 1.2f);
            caster.getWorld().spawnParticle(Particle.CRIT,
                caster.getLocation().add(0, 1, 0),
                5, 0.8, 0.4, 0.8, 0.08);

            lastBlockedTick = Bukkit.getCurrentTick();
        }
        return super.onDamage(attacker, damage, type);
    }

    private boolean isFrontFacing(LivingEntity attacker) {
        if (attacker == null) return false;

        double angle = Math.abs(caster.getLocation().getDirection().angle(
            attacker.getLocation().subtract(caster.getLocation()).toVector()));
        return angle <= Math.PI / 2;
    }

    private void healNearbyAllies() {
        double healingPower = Math.min(100, Math.max(0,
                caster.getAttribute(Attribute.STAT_HEALINGPOWER).getValue()));
        double amount = Math.min(caster.getMaxHealth() * 0.1, healAmount + healingPower * 0.12);
        healOne(caster, amount);
        for (Entity entity : caster.getWorld().getNearbyEntities(caster.getLocation(), healRange, healRange, healRange)) {
            if (entity instanceof LivingEntity target && entity != caster && !Spellbook.canAttack(caster, target)) {
                healOne(target, amount);
            }
        }

        caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.RECORDS, 0.5f, 1.5f);
    }

    private void createHolyGroundAoE() {
        holyGroundAoE = createCircularAoE(caster.getLocation(), 3, 1, holyLightDuration)
                .followEntity(caster);
    }

    @EventHandler
    private void onStopUse(PlayerStopUsingItemEvent event) {
        if (event.getPlayer() != caster || !currentlyBlocking) {
            return;
        }
        endNow(true);
    }

    private void healOne(LivingEntity target, double amount) {
        double adjusted = HealingContext.isPlayerCombat(caster, target) ? amount * 0.65 : amount;
        target.heal(adjusted);
        TargetedParticles.spawnForOthers(Particle.HEART, target,
                target.getLocation().add(0, 2, 0), 2, 0.3, 0.3, 0.3);
    }

    private void releaseCounter() {
        if (!currentlyBlocking) return;
        boolean perfect = Bukkit.getCurrentTick() - lastBlockedTick <= 12;
        Vector facing = caster.getLocation().getDirection().setY(0).normalize();
        int hits = 0;
        for (Entity entity : caster.getWorld().getNearbyEntities(caster.getLocation(), counterRange,
                counterRange, counterRange)) {
            if (!(entity instanceof LivingEntity target) || !Spellbook.canAttack(caster, target)) continue;
            Vector toward = target.getLocation().toVector().subtract(caster.getLocation().toVector()).setY(0);
            if (toward.lengthSquared() > counterRange * counterRange || facing.dot(toward.normalize()) < 0.45) continue;
            if (!caster.hasLineOfSight(target)) continue;
            double physicalAdvantage = Math.min(100, Math.max(0,
                    caster.getAttribute(Attribute.ADVANTAGE_PHYSICAL).getValue()));
            double damage = (counterDamage + physicalAdvantage * 0.2) * (perfect ? 1.6 : 1.0);
            CombatSourceContext.runSpell(this, () -> target.damage(damage, caster, PDamageType.PHYSICAL));
            BossControl.apply(caster, target, perfect ? 55 : 30);
            hits++;
            if (hits >= 3) break;
        }
        if (perfect) healNearbyAllies();
        caster.getWorld().playSound(caster.getLocation(), perfect ? Sound.ITEM_SHIELD_BREAK :
                Sound.ITEM_SHIELD_BLOCK, SoundCategory.RECORDS, perfect ? 1.0f : 0.65f, 1.25f);
    }

    @EventHandler
    private void onItemHeld(PlayerItemHeldEvent event) {
        if (event.getPlayer() == caster) {
            stopBlocking();
            keepAliveTicks = currentTicks;
        }
    }

    @EventHandler
    private void onSwapHands(PlayerSwapHandItemsEvent event) {
        if (event.getPlayer() == caster) {
            stopBlocking();
            keepAliveTicks = currentTicks;
        }
    }

    @EventHandler
    private void onQuit(PlayerQuitEvent event) {
        if (event.getPlayer() == caster) {
            stopBlocking();
            keepAliveTicks = currentTicks;
        }
    }

    private void startBlocking() {
        if (currentlyBlocking) {
            return;
        }
        currentlyBlocking = true;
        blockingStartedTick = Bukkit.getCurrentTick();
        lastBlockedTick = -100;
        caster.getAttribute(Attribute.MOVEMENT_SPEED).removeModifier(movementSpeedModifier);
        caster.getAttribute(Attribute.MOVEMENT_SPEED).addTransientModifier(movementSpeedModifier);

        caster.getWorld().playSound(caster.getLocation(), Sound.ITEM_ARMOR_EQUIP_IRON, SoundCategory.RECORDS, 1, 0.8f);
    }

    private void stopBlocking() {
        currentlyBlocking = false;
        caster.getAttribute(Attribute.MOVEMENT_SPEED).removeModifier(movementSpeedModifier);

    }

    private void endNow(boolean counter) {
        if (ended) return;
        if (counter && currentlyBlocking) releaseCounter();
        cleanup();
        caster.getActiveSpells().remove(this);
    }

    @Override
    protected void cleanup() {
        if (ended) return;
        ended = true;
        if (currentlyBlocking) {
            stopBlocking();
        } else {
            caster.getAttribute(Attribute.MOVEMENT_SPEED).removeModifier(movementSpeedModifier);
        }
        if (holyGroundAoE != null) {
            holyGroundAoE.end();
        }
        if (blockingItem != null && !blockingItemHadBlocksAttacks) {
            blockingItem.unsetData(DataComponentTypes.BLOCKS_ATTACKS);
            if (caster instanceof Player player) {
                ItemStack held = player.getInventory().getItemInMainHand();
                if (held.getType() == blockingItem.getType()
                        && held.hasData(DataComponentTypes.BLOCKS_ATTACKS)) {
                    held.unsetData(DataComponentTypes.BLOCKS_ATTACKS);
                    player.getInventory().setItemInMainHand(held);
                }
            }
        }
        HandlerList.unregisterAll(this);
        super.cleanup();
    }
}
