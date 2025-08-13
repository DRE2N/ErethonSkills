package de.erethon.hecate.casting;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.hecate.Hecate;
import de.erethon.hecate.classes.HClass;
import de.erethon.hecate.classes.Traitline;
import de.erethon.hecate.data.HCharacter;
import de.erethon.hecate.events.CombatModeEnterEvent;
import de.erethon.hecate.events.CombatModeLeaveEvent;
import de.erethon.hecate.events.CombatModeReason;
import de.erethon.hecate.progression.LevelInfo;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellEffect;
import de.erethon.spellbook.api.SpellbookSpell;
import de.erethon.spellbook.api.TraitData;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attributable;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
    private final HashMap<Attribute, Double> cachedAttributes = new HashMap<>();

    private final NamespacedKey castingMarker = new NamespacedKey(plugin, "casting_marker");

    public CharacterCastingManager(HCharacter character) {
        this.character = character;
        this.player = character.getHPlayer().getPlayer();
    }

    public void switchCastMode(CombatModeReason reason, boolean newMode, ItemStack castingItem) {
        if (isInCastMode == newMode) {
            return;
        }
        if (!Bukkit.isPrimaryThread()) {
            Hecate.log("Attempted to switch combat mode on non-primary thread.");
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

    public SpellData getSpellAtSlot(int slot) {
        if (slot < 0 || slot >= slotSpells.length) {
            return null;
        }
        return slotSpells[slot];
    }

    private void startCastMode() {
        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                updateUI();
            }
        };
        updateTask.runTaskTimer(plugin, 0, 20);
        setAttributesForLevel();
        for (TraitData trait : combatOnlyTraits) {
            player.addTrait(trait);
        }
        for (TraitData innate : character.getTraitline().getInnateTraits()) {
            if (player.hasTrait(innate)) {
                continue;
            }
            player.addTrait(innate);
        }
        populateSlots();
        updateSkillUI();
    }

    private void stopCastMode() {
        updateTask.cancel();
        for (TraitData trait : combatOnlyTraits) {
            player.removeTrait(trait);
        }
        player.getInventory().setHeldItemSlot(previousSlot);
    }

    private void updateUI() {
        // Only do the expensive skill UI update if any attributes have changed.
        for (Attribute attribute : Registry.ATTRIBUTE) {
            if (player.getAttribute(attribute) == null) {
                continue;
            }
            if (cachedAttributes.containsKey(attribute)) {
                double value = player.getAttribute(attribute).getValue();
                if (cachedAttributes.get(attribute) != value) {
                    cachedAttributes.put(attribute, value);
                    updateSkillUI();
                }
            } else {
                cachedAttributes.put(attribute, player.getAttribute(attribute).getValue());
                updateSkillUI();
            }
        }
        // Always update cooldowns
        for (int i = 0; i < slotSpells.length; i++) {
            SpellbookSpell activeSpell = cachedActiveSpells[i];
            if (activeSpell == null || slotSpells[i] == null) {
                continue;
            }
            ItemStack stack = player.getInventory().getItem(i);
            if (stack == null || !stack.getPersistentDataContainer().has(castingMarker)) {
                continue; // Skip if the item is not a casting item
            }
            SpellData spellData = activeSpell.getData();
            int cooldown = spellData.getCooldown();

            if (player.getUsedSpells().containsKey(spellData)) {
                int current = getCooldownFromTimeStamp(player.getUsedSpells().get(spellData));
                int remaining = cooldown - current;
                stack.setAmount(Math.max(1, remaining));

                if (!player.hasCooldown(stack.getType())) {
                    player.setCooldown(stack, getRemainingCooldownTicks(current, cooldown));
                }
            } else {
                if (stack.getAmount() != 1) {
                    stack.setAmount(1);
                }
                if (player.hasCooldown(stack.getType())) {
                    player.setCooldown(stack, 0);
                }
            }
        }
        // Always update HUD
        int energy = player.getEnergy();
        int maxEnergy = player.getMaxEnergy();
        double health = player.getHealth();
        double maxHealth = player.getMaxHealth();

        Component healthIcon = Component.text("\u2665", NamedTextColor.RED);
        Component healthNumbers = Component.text(String.format("%.1f", health) + "/" + String.format("%.1f", maxHealth), NamedTextColor.RED);
        Component healthText = healthIcon.append(Component.space()).append(healthNumbers);

        Traitline traitline = character.getTraitline();
        TextColor energyColor = traitline.getEnergyColor();
        String energyIconUnicode = traitline.getEnergySymbol();
        Component energyIcon = Component.text(energyIconUnicode, energyColor);
        Component energyNumbers = Component.text(energy + "/" + maxEnergy, energyColor);
        Component energyText = energyIcon.append(Component.space()).append(energyNumbers);

        StringBuilder positiveEffectsBuilder = new StringBuilder();
        StringBuilder negativeEffectsBuilder = new StringBuilder();

        for (SpellEffect effect : player.getEffects()) {
            if (effect.data.isPositive()) {
                positiveEffectsBuilder.append("+").append(effect.data.getIcon()).append(" ");
            } else {
                negativeEffectsBuilder.append("-").append(effect.data.getIcon()).append(" ");
            }
        }
        Component positiveEffectsComponent = Component.text(positiveEffectsBuilder.toString().trim(), NamedTextColor.GREEN);
        Component negativeEffectsComponent = Component.text(negativeEffectsBuilder.toString().trim(), NamedTextColor.RED);

        // Separators
        Component centerSpacer = Component.text(" | ", NamedTextColor.DARK_GRAY);
        Component betweenSpacer = Component.text("     ");

        // --- Calculate Content Widths on Each Side of the Separator ---

        // Left side content: Health + BetweenSpacer + Positive Effects
        PlainTextComponentSerializer pt = PlainTextComponentSerializer.plainText();
        int healthWidth = pt.serialize(healthText).length();
        int posEffectsWidth = pt.serialize(positiveEffectsComponent).length();
        int betweenSpacerWidth = pt.serialize(betweenSpacer).length();

        int leftContentWidth = healthWidth + betweenSpacerWidth + posEffectsWidth;

        // Right side content: Negative Effects + BetweenSpacer + Energy
        int negEffectsWidth = pt.serialize(negativeEffectsComponent).length();
        int energyWidth = pt.serialize(energyText).length();

        int rightContentWidth = negEffectsWidth + betweenSpacerWidth + energyWidth;
        int requiredSideWidth = Math.max(leftContentWidth, rightContentWidth);
        int leftPaddingSpaces = Math.max(0, requiredSideWidth - leftContentWidth);
        int rightPaddingSpaces = Math.max(0, requiredSideWidth - rightContentWidth);

        Component leftPadding = Component.text(" ".repeat(leftPaddingSpaces));
        Component rightPadding = Component.text(" ".repeat(rightPaddingSpaces));

        Component actionbar = leftPadding
                .append(healthText)
                .append(betweenSpacer)
                .append(positiveEffectsComponent)
                .append(centerSpacer) // The anchor point that should appear in the screen center
                .append(negativeEffectsComponent)
                .append(betweenSpacer)
                .append(energyText)
                .append(rightPadding);
        player.sendActionBar(actionbar);
    }

    private void updateSkillUI() {
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
            item.editPersistentDataContainer(pdc -> pdc.set(castingMarker, PersistentDataType.BYTE, (byte) 1)); // To be 100% sure that we only modify our own items
            player.getInventory().setItem(i, item);
            slotSpells[i] = spellData;
            cachedActiveSpells[i] = spellData.getActiveSpell(player);
        }
    }

    private ItemStack getItemStackFromSpellData(int slot, SpellData spellData) {
        ItemStack item = new ItemStack(CastingStatics.SLOT_DYES[slot]);
        item.setData(DataComponentTypes.CUSTOM_NAME, Component.translatable("spellbook.spell.name." + spellData.getId()));
        List<Component> placeholders = spellData.getActiveSpell(player).getPlaceholders(player);
        List<Component> lore = new ArrayList<>();
        for (int i = 0; i < spellData.getDescriptionLineCount(); i++) {
            lore.add(Component.translatable("spellbook.spell.description." + spellData.getId() + "." + i, placeholders));
        }
        item.setData(DataComponentTypes.LORE, ItemLore.lore(lore));
        return item;
    }

    private void updateLoreFromActiveSpell(ItemStack stack, SpellbookSpell spell) {
        if (stack == null || spell == null || !stack.getPersistentDataContainer().has(castingMarker)) {
            return;
        }
        List<Component> lore = new ArrayList<>();
        for (int i = 0; i < spell.getData().getDescriptionLineCount(); i++) {
            lore.add(Component.translatable("spellbook.spell.description." + spell.getId() + "." + i, spell.getPlaceholders(player)));
        }
        stack.setData(DataComponentTypes.LORE, ItemLore.lore(lore));
    }

    private int getRemainingCooldownTicks(int current, int cooldown) {
        if (current <= 0 || cooldown <= 0) {
            return 0;
        }
        int remainingSeconds = cooldown - current;
        if (remainingSeconds <= 0) {
            return 0;
        }
        return remainingSeconds * 20;
    }

    private int getCooldownFromTimeStamp(long timestamp) {
        if (timestamp <= 0) {
            return 0;
        }
        long currentTime = System.currentTimeMillis();
        return (int) ((currentTime - timestamp) / 1000);
    }

    private void setAttributesForLevel() {
        HCharacter hCharacter = character;
        Traitline traitline = hCharacter.getTraitline();
        if (traitline == null) {
            return;
        }

        int characterLevel = hCharacter.getLevel();
        Map<Integer, LevelInfo> levelInfoMap = traitline.getLevelInfo();

        if (levelInfoMap == null || levelInfoMap.isEmpty()) {
            return;
        }

        // Calculate cumulative attribute bonuses up to the character's level
        Map<Attribute, Double> cumulativeAttributes = new HashMap<>();

        for (int level = 1; level <= characterLevel; level++) {
            LevelInfo levelInfo = levelInfoMap.get(level);
            if (levelInfo != null && levelInfo.getBaseAttributeBonus() != null) {
                for (Map.Entry<Attribute, Double> entry : levelInfo.getBaseAttributeBonus().entrySet()) {
                    Attribute attribute = entry.getKey();
                    Double bonus = entry.getValue();
                    cumulativeAttributes.merge(attribute, bonus, Double::sum);
                }
            }
        }

        Attributable defaultAttributeInstance = player.getType().getDefaultAttributes();
        for (Map.Entry<Attribute, Double> entry : cumulativeAttributes.entrySet()) {
            Attribute attribute = entry.getKey();
            Double totalBonus = entry.getValue();

            if (player.getAttribute(attribute) != null && totalBonus > 0 && defaultAttributeInstance.getAttribute(attribute) != null) {
                double defaultBase = defaultAttributeInstance.getAttribute(attribute).getBaseValue();

                double finalBaseValue = defaultBase + totalBonus;
                player.getAttribute(attribute).setBaseValue(finalBaseValue);

                Hecate.log("Set base value for " + attribute.name() + " to " +
                          finalBaseValue + " (default: " + defaultBase + " + bonus: " + totalBonus + ")");
            }
        }
    }


}
