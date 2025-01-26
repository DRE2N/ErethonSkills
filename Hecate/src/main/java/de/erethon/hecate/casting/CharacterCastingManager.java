package de.erethon.hecate.casting;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.hecate.Hecate;
import de.erethon.hecate.classes.HClass;
import de.erethon.hecate.data.HCharacter;
import de.erethon.hecate.events.CombatModeEnterEvent;
import de.erethon.hecate.events.CombatModeLeaveEvent;
import de.erethon.hecate.events.CombatModeReason;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// This class manages the casting mode of a character. It exists per character and is responsible for setting up the player's UI, updating it, and handling the player's attributes.
public class CharacterCastingManager {

    private Hecate plugin = Hecate.getInstance();
    private HCharacter character;
    private Player player;
    private boolean isInCastMode = false;
    private ItemStack castingItem;
    private int previousSlot;

    private BukkitRunnable updateTask;
    private final Set<TraitData> combatOnlyTraits = new HashSet<>();

    public CharacterCastingManager(HCharacter character) {
        this.character = character;
        this.player = character.getHPlayer().getPlayer();
    }

    public void switchCastMode(CombatModeReason reason, boolean newMode, ItemStack castingItem) {
        if (isInCastMode == newMode) {
            return;
        }
        if (!Bukkit.isPrimaryThread()) {
            MessageUtil.log("Attempted to switch combat mode on non-primary thread.");
            return;
        }
        isInCastMode = newMode;
        if (isInCastMode) {
            this.castingItem = castingItem;
            startCastMode();
            CombatModeEnterEvent event = new CombatModeEnterEvent(player, character, reason);
            Bukkit.getPluginManager().callEvent(event);
        } else {
            stopCastMode();
            CombatModeLeaveEvent event = new CombatModeLeaveEvent(player, character, reason);
            Bukkit.getPluginManager().callEvent(event);
        }
    }

    private void startCastMode() {
        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                updateUI();
            }
        };
        updateTask.runTaskTimer(plugin, 0, 20);
        setClassAttributes(character.getHClass());
        for (TraitData trait : combatOnlyTraits) {
            player.addTrait(trait);
        }
        populateSlots();
    }

    private void stopCastMode() {
        updateTask.cancel();
        for (TraitData trait : combatOnlyTraits) {
            player.removeTrait(trait);
        }
        player.getInventory().setHeldItemSlot(previousSlot);
    }

    private void updateUI() {
        // Update the player's UI
    }

    private void populateSlots() {
        // Fully clear the hotbar. Its saved with the player's character data so we don't need to worry about removing items that were there before.
        for (int i = 0; i < 9; i++) {
            player.getInventory().setItem(i, null);
        }
        // Add the weapon to its place
        previousSlot = player.getInventory().getHeldItemSlot();
        player.getInventory().setHeldItemSlot(8);
        player.getInventory().setItem(8, castingItem);
        // Populate the player's slots
    }

    private void setClassAttributes(HClass hClass) {
        if (hClass == null) {
            return;
        }
        int level = character.getLevel();
        if (hClass.getAttributesPerLevel(level) == null || hClass.getAttributesPerLevel(level).isEmpty()) { // We don't need to increase attributes every level
            return;
        }
        for (Map.Entry<Attribute, Double> entry : hClass.getAttributesPerLevel(level).entrySet()) {
            MessageUtil.log("Setting " + entry.getKey().getKey() + " to " + entry.getValue() + " for " + player.getName());
            if (player.getAttribute(entry.getKey()) == null) {
                MessageUtil.log("Attribute " + entry.getKey().getKey() + " not found on Player class. Check Papyrus.");
                continue;
            }
            player.getAttribute(entry.getKey()).setBaseValue(entry.getValue());
        }
    }


}
