package de.erethon.hecate.casting;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.hecate.Hecate;
import de.erethon.hecate.classes.HClass;
import de.erethon.hecate.data.HCharacter;
import de.erethon.hecate.events.CombatModeEnterEvent;
import de.erethon.hecate.events.CombatModeLeaveEvent;
import de.erethon.hecate.events.CombatModeReason;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookSpell;
import de.erethon.spellbook.api.TraitData;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// This class manages the casting mode of a character. It exists per character and is responsible for setting up the player's UI, updating it, and handling the player's attributes.
public class CharacterCastingManager {

    private final Hecate plugin = Hecate.getInstance();
    private final HCharacter character;
    private final Player player;
    private boolean isInCastMode = false;
    private ItemStack castingItem;
    private int previousSlot;

    private BukkitRunnable updateTask;
    private final Set<TraitData> combatOnlyTraits = new HashSet<>();
    private final SpellData[] slotSpells = new SpellData[8];
    private final SpellbookSpell[] cachedActiveSpells = new SpellbookSpell[8];

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
        for (int i = 0; i < slotSpells.length; i++) {
            if (cachedActiveSpells[i] == null || slotSpells[i] == null) {
                continue;
            }
            if (slotSpells[i] != cachedActiveSpells[i].getData()) {
                // The active spell has changed, update the cache. Might happen with spells that can be re-triggered.
                cachedActiveSpells[i] = slotSpells[i].getActiveSpell(player);
                continue;
            }
            // Update lore
            SpellbookSpell activeSpell = cachedActiveSpells[i];
            ItemStack stack = player.getInventory().getItem(i);
            if (stack == null) {
                continue;
            }
            updateLoreFromActiveSpell(stack, activeSpell);
            // Update cooldown
            int cooldown = activeSpell.getData().getCooldown();
            int current = getCooldownFromTimeStamp(player.getUsedSpells().get(activeSpell.getData()));
            stack.setAmount(getCooldownPercentage(cooldown, current));
            player.setCooldown(stack, cooldown);
        }
    }

    private void populateSlots() {
        // Fully clear the hotbar. It's saved with the player's character data, so we don't need to worry about removing items that were there before.
        for (int i = 0; i < 9; i++) {
            player.getInventory().setItem(i, null);
        }
        // Add the weapon to its place
        previousSlot = player.getInventory().getHeldItemSlot();
        player.getInventory().setHeldItemSlot(8);
        player.getInventory().setItem(8, castingItem);
        List<SpellData> traitlineSpells = character.getTraitline().defaultSpellSlots;
        // Populate the player's slots
        for (int i = 0; i < 8; i++) {
            if (i >= traitlineSpells.size()) {
                break;
            }
            SpellData spellData = traitlineSpells.get(i);
            if (spellData == null) {
                continue;
            }
            ItemStack item = getItemStackFromSpellData(i, spellData);
            player.getInventory().setItem(i, item);
            slotSpells[i] = spellData;
            cachedActiveSpells[i] = spellData.getActiveSpell(player);
        }
    }

    private ItemStack getItemStackFromSpellData(int slot, SpellData spellData) {
        ItemStack item = new ItemStack(CastingStatics.SLOT_DYES[slot]);
        item.setData(DataComponentTypes.CUSTOM_NAME, Component.translatable("spellbook.spell.name" + spellData.getId()));
        List<Component> placeholders = spellData.getActiveSpell(player).getPlaceholders(player);
        List<Component> lore = new ArrayList<>();
        for (int i = 0; i < spellData.getDescriptionLineCount(); i++) {
            lore.add(Component.translatable("spellbook.spell.description." + spellData.getId() + "." + i, placeholders));
        }
        item.setData(DataComponentTypes.LORE, ItemLore.lore(lore));
        return item;
    }

    private void updateLoreFromActiveSpell(ItemStack stack, SpellbookSpell spell) {
        if (stack == null || spell == null) {
            return;
        }
        List<Component> lore = new ArrayList<>();
        for (int i = 0; i < spell.getData().getDescriptionLineCount(); i++) {
            lore.add(Component.translatable("spellbook.spell.description." + spell.getId() + "." + i, spell.getPlaceholders(player)));
        }
        stack.setData(DataComponentTypes.LORE, ItemLore.lore(lore));
    }

    private int getCooldownPercentage(int current, int cooldown) {
        if (current <= 0 || cooldown <= 0) {
            return 0;
        }
        return (int) ((double) current / cooldown * 100);
    }

    private int getCooldownFromTimeStamp(long timestamp) {
        if (timestamp <= 0) {
            return 0;
        }
        long currentTime = System.currentTimeMillis();
        return (int) ((currentTime - timestamp) / 1000);
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
