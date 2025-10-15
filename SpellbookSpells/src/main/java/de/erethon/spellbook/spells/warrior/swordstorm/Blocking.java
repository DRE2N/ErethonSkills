package de.erethon.spellbook.spells.warrior.swordstorm;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookSpell;
import de.erethon.spellbook.spells.warrior.WarriorBaseSpell;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.BlocksAttacks;
import io.papermc.paper.event.player.PlayerStopUsingItemEvent;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class Blocking extends SwordstormBaseSpell implements Listener {

    // RMB: Block with your sword, reducing incoming damage but also reducing your movement speed.

    private final double blockDamageMultiplier = data.getDouble("blockDamageMultiplier", 0.5);
    private final double movementSpeedReduction = data.getDouble("movementSpeedReduction", -0.05);

    private final AttributeModifier movementSpeedModifier = new AttributeModifier(NamespacedKey.fromString("spellbook:blocking"), movementSpeedReduction, AttributeModifier.Operation.ADD_NUMBER);
    private boolean currentlyBlocking = false;

    public Blocking(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = -1; // Keep the spell alive until manually stopped
    }

    @Override
    protected boolean onPrecast() {
        for (SpellbookSpell spell : caster.getActiveSpells()) { // Prevent double casts
            if (spell instanceof Blocking) {
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
        BlocksAttacks blocksAttacks = BlocksAttacks.blocksAttacks().build();
        mainHand.setData(DataComponentTypes.BLOCKS_ATTACKS, blocksAttacks);
        Bukkit.getPluginManager().registerEvents(this, Spellbook.getInstance().getImplementer());
        return super.onCast();
    }

    @Override
    public double onDamage(LivingEntity attacker, double damage, PDamageType type) {
        if (currentlyBlocking) {
            damage *= blockDamageMultiplier;
            caster.getWorld().playSound(caster.getLocation(), Sound.ITEM_SHIELD_BLOCK, SoundCategory.RECORDS, 1, 1.0f);
        }
        return super.onDamage(attacker, damage, type);
    }

    // TODO: This might need some Papyrus changes for blocks that have interactions (e.g. our note blocks)
    @EventHandler
    private void onUse(PlayerInteractEvent event) {
        if (event.getPlayer() != caster) {
            return;
        }
        ItemStack item = event.getPlayer().getEquipment().getItemInMainHand();
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && item.hasData(DataComponentTypes.BLOCKS_ATTACKS)) {
            if (!currentlyBlocking) {
                currentlyBlocking = true;
                caster.getAttribute(Attribute.MOVEMENT_SPEED).addTransientModifier(movementSpeedModifier);
                event.setUseInteractedBlock(Event.Result.DENY);
            }
            return;
        }
        if (event.getAction() == Action.RIGHT_CLICK_AIR && item.hasData(DataComponentTypes.BLOCKS_ATTACKS)) {
            if (!currentlyBlocking) {
                currentlyBlocking = true;
                caster.getAttribute(Attribute.MOVEMENT_SPEED).addTransientModifier(movementSpeedModifier);
            }
            return;
        }
    }

    @EventHandler
    private void onStopUse(PlayerStopUsingItemEvent event) {
        if (event.getPlayer() != caster || !currentlyBlocking) {
            return;
        }
        currentlyBlocking = false;
        caster.getAttribute(Attribute.MOVEMENT_SPEED).removeModifier(movementSpeedModifier);
    }
}
