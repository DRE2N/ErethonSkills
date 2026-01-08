package de.erethon.hecate.casting;

import de.erethon.aergia.Aergia;
import de.erethon.aergia.player.EPlayer;
import de.erethon.aergia.ui.UIComponent;
import de.erethon.aergia.ui.UIUpdater;
import de.erethon.hecate.Hecate;
import de.erethon.hecate.classes.Traitline;
import de.erethon.hecate.data.HCharacter;
import de.erethon.hecate.events.CombatModeEnterEvent;
import de.erethon.hecate.events.CombatModeLeaveEvent;
import de.erethon.hecate.events.CombatModeReason;
import de.erethon.hecate.progression.LevelInfo;
import de.erethon.hecate.ui.EffectDisplayFormatter;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookSpell;
import de.erethon.spellbook.api.TraitData;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.Listener;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attributable;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.map.MinecraftFont;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// This class manages the casting mode of a character. It exists per character and is responsible for setting up the player's UI, updating it, and handling the player's attributes.
public class CharacterCastingManager implements Listener {

    private final Hecate plugin = Hecate.getInstance();
    private final HCharacter character;
    private final Player player;
    private final NamespacedKey PERSISTENT_CASTING_KEY = new NamespacedKey("spellbook", "cast_mode");
    private boolean isInCastMode = false;
    private ItemStack castingItem;
    private ItemStack originalCastingItem; // Store the original weapon
    private int previousSlot;
    private boolean scaledPvPMode = false;
    private static final int LEVEL_FOR_SCALED_PVP = 18;

    private BukkitRunnable updateTask;
    private final Set<TraitData> combatOnlyTraits = new HashSet<>();
    private final SpellData[] slotSpells = new SpellData[8];
    private final SpellbookSpell[] cachedActiveSpells = new SpellbookSpell[8];
    private final HashMap<Attribute, Double> cachedAttributes = new HashMap<>();
    private UIUpdater uiUpdater;

    private final NamespacedKey castingMarker = new NamespacedKey(plugin, "casting_marker");
    private long castingItemNameSetTick = -1;

    // Global caches to prevent recreation on first entry
    private static final Component HEALTH_ICON = Component.text("\u2665", NamedTextColor.RED);
    private static final Component BETWEEN_SPACER = Component.text("     ");
    private static final Component DARK_GRAY_OPEN_BRACKET = Component.text("[", NamedTextColor.DARK_GRAY);
    private static final Component DARK_GRAY_CLOSE_BRACKET = Component.text("]", NamedTextColor.DARK_GRAY);
    private static final Component SPACE = Component.space();
    private static final Component EMPTY_COMPONENT = Component.empty();
    private static final Component RMB_TEXT = Component.text("RMB", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false);
    private static final String DEFAULT_ENERGY_ICON = "\u26A1";
    private static final Key AIR_MODEL_KEY = Key.key("minecraft:air");
    private static final TooltipDisplay HIDDEN_TOOLTIP = TooltipDisplay.tooltipDisplay().hideTooltip(true).build();

    private static final Map<String, Map<String, Component>> SPELL_NAME_CACHE = new HashMap<>();

    // Key: spellId + "." + lineIndex -> Component
    private static final Map<String, Component> SPELL_DESCRIPTION_CACHE = new HashMap<>();

    private static final ItemStack[] EMPTY_STACK_CACHE = new ItemStack[9];

    public CharacterCastingManager(HCharacter character) {
        this.character = character;
        this.player = character.getHPlayer().getPlayer();
        Bukkit.getPluginManager().registerEvents(this, plugin);
        loadUIUpdater();
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
            // Store the original and create a copy for cast mode
            this.originalCastingItem = castingItem;
            this.castingItem = castingItem != null ? castingItem.clone() : null;
            startCastMode();
            CombatModeEnterEvent event = new CombatModeEnterEvent(player, character, reason);
            Bukkit.getPluginManager().callEvent(event);
        } else {
            stopCastMode();
            CombatModeLeaveEvent event = new CombatModeLeaveEvent(player, character, reason);
            Bukkit.getPluginManager().callEvent(event);
            // Clear references
            this.castingItem = null;
            this.originalCastingItem = null;
        }
    }

    public SpellData getSpellAtSlot(int slot) {
        if (slot < 0 || slot >= slotSpells.length) {
            return null;
        }
        return slotSpells[slot];
    }

    public void updateCastingItemName(SpellData data) {
        if (data == null) {
            castingItem.setData(DataComponentTypes.CUSTOM_NAME, EMPTY_COMPONENT);
            player.getInventory().setItem(8, castingItem);
            return;
        }
        Traitline traitline = character.getTraitline();
        TextColor energyColor = traitline != null ? traitline.getEnergyColor() : null;
        Component spellName = getCachedSpellName(data.getId(), energyColor);
        Component energyCostText = EMPTY_COMPONENT;
        if (data.get("energyCost") != null) {
            int energyCost = data.getInt("energyCost", 0);
            energyCostText = DARK_GRAY_OPEN_BRACKET
                    .append(Component.text("-" + energyCost, energyColor != null ? energyColor : NamedTextColor.WHITE))
                    .append(DARK_GRAY_CLOSE_BRACKET);
        }
        Component name = spellName.append(SPACE).append(energyCostText);
        castingItem.setData(DataComponentTypes.CUSTOM_NAME, name);

        castingItemNameSetTick = player.getTicksLived();
        player.getInventory().setItem(8, castingItem);
    }

    private void updateCastingItemLore(SpellData rightClickSpell) {
        List<Component> lore = new ArrayList<>();

        // Attack modifier description
        Traitline traitline = character.getTraitline();
        if (traitline != null) {
            lore.addAll(traitline.getAttackModifierDescription());
        }
        lore.add(SPACE);

        // RMB spell description
        if (rightClickSpell != null) {
            lore.add(RMB_TEXT);
            SpellbookSpell activeSpell = rightClickSpell.getActiveSpell(player);
            TextColor energyColor = traitline != null ? traitline.getEnergyColor() : null;
            Component name = getCachedSpellName(rightClickSpell.getId(), energyColor);
            lore.add(name);
            List<Component> placeholders = activeSpell.getPlaceholders(player);
            for (int i = 0; i < rightClickSpell.getDescriptionLineCount(); i++) {
                lore.add(getCachedSpellDescription(rightClickSpell.getId(), i, placeholders));
            }
        }

        castingItem.setData(DataComponentTypes.LORE, ItemLore.lore(lore));
        player.getInventory().setItem(8, castingItem);
    }

    private void startCastMode() {
        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                updateUI();
            }
        };
        updateTask.runTaskTimer(plugin, 0, 20);
        int level = character.getLevel();
        if (scaledPvPMode && level < LEVEL_FOR_SCALED_PVP) {
            level = LEVEL_FOR_SCALED_PVP;
        }
        setAttributesForLevel(level);
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
        player.getPersistentDataContainer().set(PERSISTENT_CASTING_KEY, PersistentDataType.BOOLEAN, true);
    }

    private void stopCastMode() {
        updateTask.cancel();
        for (TraitData trait : combatOnlyTraits) {
            player.removeTrait(trait);
        }
        player.getInventory().setHeldItemSlot(previousSlot);
        player.getPersistentDataContainer().remove(PERSISTENT_CASTING_KEY);
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

        // Update cooldown for casting item (RIGHT_CLICK spell)
        SpellData rightClickSpell = character.getTraitline().getSpecialAction(SpecialActionKey.RIGHT_CLICK);
        if (rightClickSpell != null && castingItem != null) {
            ItemStack stack = player.getInventory().getItem(8);
            if (stack == null) {
                return; // Casting item is not in the inventory
            }
            int cooldown = rightClickSpell.getCooldown();
            if (player.getUsedSpells().containsKey(rightClickSpell)) {
                int current = getCooldownFromTimeStamp(player.getUsedSpells().get(rightClickSpell));
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
        int health = (int) player.getHealth();
        int maxHealth = (int) player.getMaxHealth();

        Component healthNumbers = Component.text(health + "/" + maxHealth, NamedTextColor.RED);
        Component healthText = HEALTH_ICON.append(SPACE).append(healthNumbers);

        Traitline traitline = character.getTraitline();
        if (traitline == null) {
            player.sendParsedActionBar("<red>Internal error: No traitline found.");
            return;
        }
        TextColor energyColor = traitline.getEnergyColor();
        String energyIconUnicode = traitline.getEnergySymbol();
        if (energyIconUnicode == null || energyIconUnicode.isBlank()) {
            energyIconUnicode = DEFAULT_ENERGY_ICON;
        }
        Component energyIcon = Component.text(energyIconUnicode, energyColor);
        Component energyNumbers = Component.text(energy + "/" + maxEnergy, energyColor);
        Component energyText = energyIcon.append(SPACE).append(energyNumbers);

        Component[] formatted = EffectDisplayFormatter.formatEffects(player.getEffects());
        updateBossbarUI(formatted[0], formatted[1]);

        Component actionbar = healthText
                .append(BETWEEN_SPACER)
                .append(energyText);
        actionbar = actionbar.shadowColor(ShadowColor.none());
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
        // Reset the the casting item name after 5 seconds
        if (player.getTicksLived() + 100 > castingItemNameSetTick) {
            updateCastingItemName(null);
        }
    }

    private void populateSlots() {
        // Fully clear the hotbar. It's saved with the player's character data, so we don't need to worry about removing items that were there before.
        for (int i = 0; i < 9; i++) {
            player.getInventory().setItem(i, null);
        }
        // Clear the offhand in cast mode
        player.getInventory().setItemInOffHand(null);

        // Add the weapon to its place
        previousSlot = player.getInventory().getHeldItemSlot();
        player.getInventory().setHeldItemSlot(8);
        player.getInventory().setItem(8, castingItem);
        if (character.getTraitline() != null && character.getTraitline().getSpecialAction(SpecialActionKey.RIGHT_CLICK) != null) {
            updateCastingItemLore(character.getTraitline().getSpecialAction(SpecialActionKey.RIGHT_CLICK));
        }
        List<SpellData> traitlineSpells = character.getTraitline().defaultSpellSlots;
        // Populate the player's slots
        for (int i = 0; i < 8; i++) {
            if (i >= traitlineSpells.size()) {
                // If there are not enough spells, fill the rest with empty items.
                player.getInventory().setItem(i, emptyStack());
                slotSpells[i] = null;
                cachedActiveSpells[i] = null;
                continue;
            }
            SpellData spellData = traitlineSpells.get(i);
            if (spellData == null) {
                // Add an empty, invisible item so pickups don't go into the hotbar.
                player.getInventory().setItem(i, emptyStack());
                continue;
            }
            ItemStack item = getItemStackFromSpellData(i, spellData);
            item.editPersistentDataContainer(pdc -> pdc.set(castingMarker, PersistentDataType.BYTE, (byte) 1)); // To be 100% sure that we only modify our own items
            player.getInventory().setItem(i, item);
            slotSpells[i] = spellData;
            cachedActiveSpells[i] = spellData.getActiveSpell(player);
        }
    }

    private ItemStack emptyStack() {
        int emptySlot = 8; // Use slot 8 for the cache since it's for empty items
        if (EMPTY_STACK_CACHE[emptySlot] != null) {
            return EMPTY_STACK_CACHE[emptySlot].clone();
        }

        ItemStack emptyItem = new ItemStack(CastingStatics.SLOT_DYES[8]);
        emptyItem.setData(DataComponentTypes.ITEM_MODEL, AIR_MODEL_KEY);
        emptyItem.setData(DataComponentTypes.TOOLTIP_DISPLAY, HIDDEN_TOOLTIP);
        EMPTY_STACK_CACHE[emptySlot] = emptyItem.clone();
        return emptyItem;
    }

    private ItemStack getItemStackFromSpellData(int slot, SpellData spellData) {
        ItemStack item = new ItemStack(CastingStatics.SLOT_DYES[slot]);
        Traitline traitline = character.getTraitline();
        TextColor energyColor = traitline != null ? traitline.getEnergyColor() : null;
        Component name = getCachedSpellName(spellData.getId(), energyColor);
        item.setData(DataComponentTypes.CUSTOM_NAME, name);
        item.setData(DataComponentTypes.ITEM_MODEL, NamespacedKey.fromString("erethon:spellbook_icon_" + spellData.getId().toLowerCase()));
        List<Component> placeholders = spellData.getActiveSpell(player).getPlaceholders(player);
        List<Component> lore = new ArrayList<>();
        for (int i = 0; i < spellData.getDescriptionLineCount(); i++) {
            lore.add(getCachedSpellDescription(spellData.getId(), i, placeholders));
        }
        item.setData(DataComponentTypes.LORE, ItemLore.lore(lore));
        return item;
    }

    private void updateLoreFromActiveSpell(ItemStack stack, SpellbookSpell spell) {
        if (stack == null || spell == null || !stack.getPersistentDataContainer().has(castingMarker)) {
            return;
        }
        List<Component> lore = new ArrayList<>();
        List<Component> placeholders = spell.getPlaceholders(player);
        for (int i = 0; i < spell.getData().getDescriptionLineCount(); i++) {
            lore.add(getCachedSpellDescription(spell.getId(), i, placeholders));
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

    @SuppressWarnings("removal")
    public void setAttributesForLevel(int characterLevel) {
        HCharacter hCharacter = character;
        Traitline traitline = hCharacter.getTraitline();
        if (traitline == null) {
            return;
        }

        Map<Integer, LevelInfo> levelInfoMap = traitline.getLevelInfo();

        if (levelInfoMap == null || levelInfoMap.isEmpty()) {
            return;
        }

        // First, reset all attributes to their default values to prevent stacking
        Attributable defaultAttributeInstance = player.getType().getDefaultAttributes();
        for (Attribute attribute : Registry.ATTRIBUTE) {
            if (player.getAttribute(attribute) != null && defaultAttributeInstance.getAttribute(attribute) != null) {
                double defaultBase = defaultAttributeInstance.getAttribute(attribute).getBaseValue();
                player.getAttribute(attribute).setBaseValue(defaultBase);
            }
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

        // Apply the attribute bonuses from the current traitline
        for (Map.Entry<Attribute, Double> entry : cumulativeAttributes.entrySet()) {
            Attribute attribute = entry.getKey();
            Double totalBonus = entry.getValue();

            if (player.getAttribute(attribute) != null && defaultAttributeInstance.getAttribute(attribute) != null) {
                double defaultBase = defaultAttributeInstance.getAttribute(attribute).getBaseValue();

                double finalBaseValue = defaultBase + totalBonus;
                player.getAttribute(attribute).setBaseValue(finalBaseValue);

                Hecate.log("Set base value for " + attribute.name() + " to " +
                          finalBaseValue + " (default: " + defaultBase + " + bonus: " + totalBonus + ")");
            }
        }
        player.setHealthScaled(true);
        player.setHealthScale(20.0);
    }

    public void setScaledPvPMode(boolean scaledPvPMode) {
        this.scaledPvPMode = scaledPvPMode;
        if (isInCastMode) {
            int level = character.getLevel();
            if (scaledPvPMode && level < LEVEL_FOR_SCALED_PVP) {
                level = LEVEL_FOR_SCALED_PVP;
            }
            setAttributesForLevel(level);
            updateUI();
        }
    }

    private int getSafeTextWidth(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        try {
            if (MinecraftFont.Font.isValid(text)) {
                return MinecraftFont.Font.getWidth(text);
            }
        } catch (Exception e) {
        }

        int totalWidth = 0;
        for (char c : text.toCharArray()) {
            try {
                String charStr = String.valueOf(c);
                if (MinecraftFont.Font.isValid(charStr)) {
                    totalWidth += MinecraftFont.Font.getWidth(charStr);
                } else {
                    if (c == ' ') {
                        totalWidth += 4;
                    } else if (Character.isLetterOrDigit(c)) {
                        totalWidth += 6;
                    } else {
                        totalWidth += 5;
                    }
                }
            } catch (Exception e) {
                if (c == ' ') {
                    totalWidth += 4;
                } else {
                    totalWidth += 6;
                }
            }
        }
        return totalWidth;
    }

    public void loadUIUpdater() {
        Aergia aergia = Aergia.inst();
        EPlayer ePlayer = aergia.getEPlayerCache().getByPlayer(player);
        if (ePlayer == null) {
            Hecate.log("EPlayer is null when trying to apply UIUpdater.");
            return;
        }
        uiUpdater = ePlayer.getUIUpdater();
        uiUpdater.getBossBar().getCenter().add(UIComponent.reactivatable(p -> {
            if (!isInCastMode || p.getPlayer() != player) {
                return Component.empty();
            }
            return Component.empty(); // This no worky
        }, 20, "hecate_spell_effects"));

    }

    private void updateBossbarUI(Component positive, Component negative) {
        if (uiUpdater == null) {
            Hecate.log("Somehow the UIUpdater is null when trying to update the bossbar. Trying to find it again...");
            loadUIUpdater();
            return;
        }
        UIComponent component = uiUpdater.getBossBar().getCenter().getById("hecate_spell_effects");
        if (component == null) {
            return;
        }
        if (positive == Component.empty() && negative == Component.empty()) {
            component.setComponent(UIComponent.permanent(Component.empty()));
            return;
        }
        component.setComponent(UIComponent.temporary(getSpellEffectsComponent(positive, negative), 20));
        component.resetDuration();
    }

    private Component getSpellEffectsComponent(Component positive, Component negative) {
        final int MAX_LENGTH = 600;
        Component spacer = Component.text(" | ", NamedTextColor.DARK_GRAY);
        PlainTextComponentSerializer plainTextComponentSerializer = PlainTextComponentSerializer.plainText();

        String positiveText = positive != null ? plainTextComponentSerializer.serialize(positive) : "";
        String negativeText = negative != null ? plainTextComponentSerializer.serialize(negative) : "";

        boolean hasPositive = positive != null && !positive.equals(Component.empty()) && !positiveText.isEmpty();
        boolean hasNegative = negative != null && !negative.equals(Component.empty()) && !negativeText.isEmpty();

        int positiveWidth = hasPositive ? getSafeTextWidth(positiveText) : 0;
        int negativeWidth = hasNegative ? getSafeTextWidth(negativeText) : 0;
        int spacerWidth = getSafeTextWidth(" | ");

        int totalContentWidth = positiveWidth + negativeWidth + spacerWidth;

        if (totalContentWidth > MAX_LENGTH - 100) {
            int availableWidth = MAX_LENGTH - spacerWidth - 100;
            int halfAvailable = availableWidth / 2;

            if (hasPositive && positiveWidth > halfAvailable) {
                positive = truncateComponent(positive, halfAvailable);
                positiveText = plainTextComponentSerializer.serialize(positive);
                positiveWidth = getSafeTextWidth(positiveText);
            }

            if (hasNegative && negativeWidth > halfAvailable) {
                negative = truncateComponent(negative, halfAvailable);
                negativeText = plainTextComponentSerializer.serialize(negative);
                negativeWidth = getSafeTextWidth(negativeText);
            }
        }

        Component result = Component.empty();

        if (hasPositive) {
            result = result.append(positive);
        }

        result = result.append(spacer);

        if (hasNegative) {
            result = result.append(negative);
        }

        if (hasPositive && !hasNegative) {
            String counterSpaces = " ".repeat(Math.min(positiveWidth / 4, 50)); // Divide by ~4 since space width is ~4
            result = result.append(Component.text(counterSpaces, NamedTextColor.DARK_GRAY));
        } else if (!hasPositive && hasNegative) {
            String counterSpaces = " ".repeat(Math.min(negativeWidth / 4, 50));
            result = Component.text(counterSpaces, NamedTextColor.DARK_GRAY).append(spacer).append(negative);
        }
        result = result.shadowColor(ShadowColor.none());
        return result;
    }

    private Component truncateComponent(Component component, int maxWidth) {
        if (component == null) {
            return Component.empty();
        }

        String text = component.toString();
        if (getSafeTextWidth(text) <= maxWidth) {
            return component;
        }

        // Simple truncation - could be improved to be smarter about effect boundaries
        StringBuilder truncated = new StringBuilder();
        int currentWidth = 0;

        for (char c : text.toCharArray()) {
            int charWidth = getSafeTextWidth(String.valueOf(c));
            if (currentWidth + charWidth + getSafeTextWidth("...") > maxWidth) {
                truncated.append("...");
                break;
            }
            truncated.append(c);
            currentWidth += charWidth;
        }

        return Component.text(truncated.toString(), NamedTextColor.WHITE);
    }

    private static Component getCachedSpellName(String spellId, TextColor energyColor) {
        if (energyColor == null) {
            energyColor = NamedTextColor.WHITE;
        }

        String colorKey = energyColor.asHexString();

        Map<String, Component> colorMap = SPELL_NAME_CACHE.computeIfAbsent(colorKey, k -> new HashMap<>());

        TextColor finalEnergyColor = energyColor;
        return colorMap.computeIfAbsent(spellId, id -> {
            Component name = Component.translatable("hecate.spellbook.spell.name." + id);
            return name.decoration(TextDecoration.ITALIC, false).color(finalEnergyColor);
        });
    }

    private static Component getCachedSpellDescription(String spellId, int lineIndex, List<Component> placeholders) {
        String cacheKey = spellId + "." + lineIndex;

        Component baseComponent = SPELL_DESCRIPTION_CACHE.computeIfAbsent(cacheKey, key ->
            Component.translatable("hecate.spellbook.spell.description." + key)
        );

        if (placeholders != null && !placeholders.isEmpty()) {
            return Component.translatable("hecate.spellbook.spell.description." + cacheKey, placeholders);
        }

        return baseComponent;
    }



}
