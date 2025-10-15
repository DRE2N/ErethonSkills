package de.erethon.spellbook.spells.paladin.guardian;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.aoe.AoE;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookSpell;
import de.erethon.spellbook.spells.paladin.PaladinBaseSpell;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.BlocksAttacks;
import io.papermc.paper.event.player.PlayerStopUsingItemEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class ShieldOfFaith extends GuardianBaseSpell implements Listener {

    // RMB: The Guardian raises his shield, protecting himself from attacks from the front.
    // For blocked attacks, the Paladin and his allies are healed.

    private final double blockDamageMultiplier = data.getDouble("blockDamageMultiplier", 0.3);
    private final double movementSpeedReduction = data.getDouble("movementSpeedReduction", -0.1);
    private final double healAmount = data.getDouble("healAmount", 8.0);
    private final double healRange = data.getDouble("healRange", 12.0);
    private final int holyLightDuration = data.getInt("holyLightDuration", 60);

    private final AttributeModifier movementSpeedModifier = new AttributeModifier(
            NamespacedKey.fromString("spellbook:shield_faith"),
            movementSpeedReduction,
            AttributeModifier.Operation.ADD_NUMBER
    );

    private boolean currentlyBlocking = false;
    private AoE holyGroundAoE;
    private int healCooldownTicks = 0;

    public ShieldOfFaith(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = -1;
    }

    @Override
    protected boolean onPrecast() {
        for (SpellbookSpell spell : caster.getActiveSpells()) {
            if (spell instanceof ShieldOfFaith) {
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

        BlocksAttacks blocksAttacks = BlocksAttacks.blocksAttacks().build();
        mainHand.setData(DataComponentTypes.BLOCKS_ATTACKS, blocksAttacks);

        createHolyGroundAoE();

        Bukkit.getPluginManager().registerEvents(this, Spellbook.getInstance().getImplementer());
        return super.onCast();
    }

    @Override
    protected void onTick() {
        super.onTick();

        if (healCooldownTicks > 0) {
            healCooldownTicks--;
        }

        if (currentlyBlocking && caster.getTicksLived() % 20 == 0) {
            caster.getWorld().spawnParticle(Particle.END_ROD,
                caster.getLocation().add(0, 1, 0),
                3, 0.5, 0.5, 0.5, 0.02);
        }
    }

    @Override
    public double onDamage(LivingEntity attacker, double damage, PDamageType type) {
        if (currentlyBlocking && isFrontFacing(attacker)) {
            damage *= blockDamageMultiplier;

            caster.getWorld().playSound(caster.getLocation(), Sound.ITEM_SHIELD_BLOCK, SoundCategory.RECORDS, 1, 1.2f);
            caster.getWorld().spawnParticle(Particle.CRIT,
                caster.getLocation().add(0, 1, 0),
                8, 1, 0.5, 1, 0.1);

            if (healCooldownTicks <= 0) {
                healNearbyAllies();
                healCooldownTicks = 40;
            }
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
        for (Entity entity : caster.getWorld().getNearbyEntities(caster.getLocation(), healRange, healRange, healRange)) {
            if (entity instanceof LivingEntity target && entity != caster && !Spellbook.canAttack(caster, target)) {
                double currentHealth = target.getHealth();
                double maxHealth = target.getAttribute(Attribute.MAX_HEALTH).getValue();

                if (currentHealth < maxHealth) {
                    target.setHealth(Math.min(maxHealth, currentHealth + healAmount));
                    target.getWorld().spawnParticle(Particle.HEART,
                        target.getLocation().add(0, 2, 0),
                        2, 0.3, 0.3, 0.3, 0);
                }
            }
        }

        caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.RECORDS, 0.5f, 1.5f);
    }

    private void createHolyGroundAoE() {
        holyGroundAoE = createCircularAoE(caster.getLocation(), 3, 1, holyLightDuration)
                .onTick(aoe -> {
                    if (currentlyBlocking && caster.getTicksLived() % 10 == 0) {
                        aoe.getCenter().getWorld().spawnParticle(Particle.INSTANT_EFFECT,
                            aoe.getCenter().add(0, 0.1, 0),
                            2, 1.5, 0, 1.5, 0);
                    }
                });

        if (currentlyBlocking) {
            holyGroundAoE.addBlockChange(Material.LIGHT)
                    .sendBlockChanges();
        }
    }

    @EventHandler
    private void onUse(PlayerInteractEvent event) {
        if (event.getPlayer() != caster) {
            return;
        }
        ItemStack item = event.getPlayer().getEquipment().getItemInMainHand();

        if ((event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR)
                && item.hasData(DataComponentTypes.BLOCKS_ATTACKS)) {
            if (!currentlyBlocking) {
                startBlocking();
                if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                    event.setUseInteractedBlock(Event.Result.DENY);
                }
            }
        }
    }

    @EventHandler
    private void onStopUse(PlayerStopUsingItemEvent event) {
        if (event.getPlayer() != caster || !currentlyBlocking) {
            return;
        }
        stopBlocking();
    }

    private void startBlocking() {
        currentlyBlocking = true;
        caster.getAttribute(Attribute.MOVEMENT_SPEED).addTransientModifier(movementSpeedModifier);

        if (holyGroundAoE != null) {
            holyGroundAoE.addBlocksOnTopGroundLevel(Material.LIGHT)
                    .sendBlockChanges();
        }

        caster.getWorld().playSound(caster.getLocation(), Sound.ITEM_ARMOR_EQUIP_IRON, SoundCategory.RECORDS, 1, 0.8f);
    }

    private void stopBlocking() {
        currentlyBlocking = false;
        caster.getAttribute(Attribute.MOVEMENT_SPEED).removeModifier(movementSpeedModifier);

        if (holyGroundAoE != null) {
            holyGroundAoE.revertBlockChanges();
        }
    }

    protected void onEnd() {
        if (currentlyBlocking) {
            stopBlocking();
        }
        if (holyGroundAoE != null) {
            holyGroundAoE.revertBlockChanges();
        }
        super.cleanup();
    }
}
