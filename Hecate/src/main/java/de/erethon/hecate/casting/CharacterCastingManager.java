package de.erethon.hecate.casting;

import de.erethon.aergia.Aergia;
import de.erethon.aergia.player.EPlayer;
import de.erethon.aergia.ui.UIComponent;
import de.erethon.aergia.ui.UIUpdater;
import de.erethon.hecate.Hecate;
import de.erethon.hecate.classes.HClass;
import de.erethon.hecate.classes.Traitline;
import de.erethon.hecate.data.HCharacter;
import de.erethon.hecate.events.CombatModeEnterEvent;
import de.erethon.hecate.events.CombatModeLeaveEvent;
import de.erethon.hecate.events.CombatModeReason;
import de.erethon.hecate.progression.LevelInfo;
import de.erethon.hecate.ui.EffectDisplayFormatter;
import de.erethon.hecate.ui.CombatHud;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookSpell;
import de.erethon.spellbook.api.TraitData;
import de.erethon.spellbook.spells.warrior.swordstorm.Blocking;
import de.erethon.spellbook.utils.SpellbookFeedback;
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
import org.bukkit.Material;
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
    private ItemStack combatOffhand;
    private int previousSlot;
    private boolean scaledLevelMode = false;
    private static final int LEVEL_FOR_SCALED_PVP = 18;
    private int scaledLevel = LEVEL_FOR_SCALED_PVP;

    private BukkitRunnable updateTask;
    private int periodicUiTicks;
    private final Set<TraitData> combatOnlyTraits = new HashSet<>();
    private final SpellData[] slotSpells = new SpellData[8];
    private final SpellbookSpell[] cachedActiveSpells = new SpellbookSpell[8];
    private final HashMap<Attribute, Double> cachedAttributes = new HashMap<>();
    private UIUpdater uiUpdater;

    private final NamespacedKey castingMarker = new NamespacedKey(plugin, "casting_marker");
    private long castingItemNameSetTick = -1;

    // Global caches to prevent recreation on first entry
    private static final Component DARK_GRAY_OPEN_BRACKET = Component.text("[", NamedTextColor.DARK_GRAY);
    private static final Component DARK_GRAY_CLOSE_BRACKET = Component.text("]", NamedTextColor.DARK_GRAY);
    private static final Component SPACE = Component.space();
    private static final Component EMPTY_COMPONENT = Component.empty();
    private static final Component RMB_TEXT = Component.text("RMB", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false);
    private static final Key AIR_MODEL_KEY = Key.key("minecraft:air");
    private static final TooltipDisplay HIDDEN_TOOLTIP = TooltipDisplay.tooltipDisplay().hideTooltip(true).build();

    private static final Map<String, Map<String, Component>> SPELL_NAME_CACHE = new HashMap<>();

    // Key: spellId + "." + lineIndex -> Component
    private static final Map<String, Component> SPELL_DESCRIPTION_CACHE = new HashMap<>();

    // Key: spellId -> max valid line count
    private static final Map<String, Integer> VALID_DESCRIPTION_LINE_CACHE = new HashMap<>();

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
            ItemStack equippedOffhand = player.getInventory().getItemInOffHand();
            this.combatOffhand = equippedOffhand == null ? null : equippedOffhand.clone();
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
            this.combatOffhand = null;
        }
    }

    public SpellData getSpellAtSlot(int slot) {
        if (slot < 0 || slot >= slotSpells.length) {
            return null;
        }
        return slotSpells[slot];
    }

    /** Update the active bar after a character level changes, without rebuilding the hotbar. */
    public void refreshUnlockedSkills(int characterLevel) {
        if (!isInCastMode || !Bukkit.isPrimaryThread()) return;
        Traitline traitline = character.getTraitline();
        if (traitline == null) return;
        List<SpellData> defaults = traitline.defaultSpellSlots;
        for (int i = 0; i < slotSpells.length; i++) {
            SpellData equipped = slotSpells[i];
            if (equipped != null && characterLevel < traitline.getSpellUnlockLevel(equipped)) {
                slotSpells[i] = null;
                cachedActiveSpells[i] = null;
                player.getInventory().setItem(i, emptyStack());
                equipped = null;
            }
            if (equipped != null || i >= defaults.size()) continue;
            SpellData available = defaults.get(i);
            if (available == null || characterLevel < traitline.getSpellUnlockLevel(available)) continue;
            ItemStack item = getItemStackFromSpellData(i, available);
            item.editPersistentDataContainer(pdc -> pdc.set(castingMarker, PersistentDataType.BYTE, (byte) 1));
            player.getInventory().setItem(i, item);
            slotSpells[i] = available;
            cachedActiveSpells[i] = available.getActiveSpell(player);
        }
        updateSkillUI();
    }

    /** Rebuild the active combat bar after an admin changes class or discipline. */
    public void refreshDiscipline() {
        if (!isInCastMode || !Bukkit.isPrimaryThread()) return;
        int savedPreviousSlot = previousSlot;
        endActiveSpells();
        if (updateTask != null) updateTask.cancel();
        castingItem = originalCastingItem == null ? null : originalCastingItem.clone();
        for (int i = 0; i < slotSpells.length; i++) {
            slotSpells[i] = null;
            cachedActiveSpells[i] = null;
        }
        cachedAttributes.clear();
        try {
            startCastMode();
        } finally {
            previousSlot = savedPreviousSlot;
        }
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
                Component description = getCachedSpellDescription(rightClickSpell.getId(), i, placeholders);
                if (isValidTranslation(description, rightClickSpell.getId(), i)) {
                    lore.add(description);
                } else {
                    break; // Stop adding lines once we hit a missing translation
                }
            }
        }

        castingItem.setData(DataComponentTypes.LORE, ItemLore.lore(lore));
        player.getInventory().setItem(8, castingItem);
    }

    private void startCastMode() {
        int level = character.getLevel();
        character.ensureUnlockedTraitline(level);
        character.getTraitline().refreshSpellbookReferences();
        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                updateHud();
                periodicUiTicks++;
                if (periodicUiTicks >= 20) {
                    periodicUiTicks = 0;
                    updateUI();
                }
            }
        };
        periodicUiTicks = 19; // Run the periodic UI update on the first tick as well.
        updateTask.runTaskTimer(plugin, 0, 1);
        int movementSpeedLevel = level;
        if (scaledLevelMode) {
            level = scaledLevel;
        }
        Hecate.log("Entering combat mode for " + character.getCharacterID()
                + ": archetype=" + character.getClassId()
                + ", discipline=" + character.getTraitline().getId()
                + ", level=" + level
                + ", configuredDefaultSpells=" + character.getTraitline().getStringList("defaultSpellSlots").size()
                + ", defaultSpells=" + character.getTraitline().defaultSpellSlots.size()
                + ", configuredInnateTraits=" + character.getTraitline().getStringList("innateTraits").size()
                + ", innateTraits=" + character.getTraitline().getInnateTraits().size()
                + ", configuredLevels=" + character.getTraitline().getLevelInfo().size());
        setAttributesForLevel(level, movementSpeedLevel);
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
        endActiveSpells();
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

    }

    private void endActiveSpells() {
        for (SpellbookSpell spell : List.copyOf(player.getActiveSpells())) {
            try {
                spell.interrupt();
                spell.tick(); // Spellbook's interrupted tick invokes cleanup but does not remove the spell.
            } catch (Exception e) {
                Hecate.log("Failed to end active spell " + spell.getId() + " while changing combat state: "
                        + e.getMessage());
            } finally {
                player.getActiveSpells().remove(spell);
            }
        }
    }

    public boolean isInCastMode() {
        return isInCastMode;
    }

    private void updateHud() {
        Traitline traitline = character.getTraitline();
        if (traitline == null) {
            player.sendParsedActionBar("<red>Internal error: No traitline found.");
            return;
        }
        if (!SpellbookFeedback.isVisible(player)) {
            player.sendActionBar(CombatHud.render(player, traitline));
        }

        Component[] formatted = EffectDisplayFormatter.formatEffects(player.getEffects());
        updateBossbarUI(formatted[0], formatted[1]);
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
        // Vanguard keeps ordinary shield timing even while its three skill slots are visible.
        if ("WarriorVanguard".equals(character.getTraitline().getId())) {
            player.getInventory().setItemInOffHand(combatOffhand != null
                    && combatOffhand.getType() == Material.SHIELD
                    ? combatOffhand.clone() : new ItemStack(Material.SHIELD));
        } else {
            player.getInventory().setItemInOffHand(null);
        }

        // Add the weapon to its place
        previousSlot = player.getInventory().getHeldItemSlot();
        player.getInventory().setHeldItemSlot(8);
        if (castingItem != null && "WarriorSwordstorm".equals(character.getTraitline().getId())) {
            castingItem.setData(DataComponentTypes.BLOCKS_ATTACKS, Blocking.nativeSwordBlock());
        }
        player.getInventory().setItem(8, castingItem);
        if (character.getTraitline() != null && character.getTraitline().getSpecialAction(SpecialActionKey.RIGHT_CLICK) != null) {
            updateCastingItemLore(character.getTraitline().getSpecialAction(SpecialActionKey.RIGHT_CLICK));
        }
        List<SpellData> traitlineSpells = character.getTraitline().defaultSpellSlots;
        int characterLevel = character.getLevel();
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
            if (spellData != null && characterLevel < character.getTraitline().getSpellUnlockLevel(spellData)) {
                player.getInventory().setItem(i, emptyStack());
                slotSpells[i] = null;
                cachedActiveSpells[i] = null;
                continue;
            }
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
            Component description = getCachedSpellDescription(spellData.getId(), i, placeholders);
            if (isValidTranslation(description, spellData.getId(), i)) {
                lore.add(description);
            } else {
                break; // Stop adding lines once we hit a missing translation
            }
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
            Component description = getCachedSpellDescription(spell.getId(), i, placeholders);
            if (isValidTranslation(description, spell.getId(), i)) {
                lore.add(description);
            } else {
                break; // Stop adding lines once we hit a missing translation
            }
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
        setAttributesForLevel(characterLevel, characterLevel);
    }

    @SuppressWarnings("removal")
    private void setAttributesForLevel(int characterLevel, int movementSpeedLevel) {
        HCharacter hCharacter = character;
        Traitline traitline = hCharacter.getTraitline();
        if (traitline == null) {
            return;
        }

        Map<Integer, LevelInfo> levelInfoMap = traitline.getLevelInfo();
        HClass characterClass = hCharacter.getHClass();
        Map<Attribute, HClass.AttributeCurve> attributeCurves = characterClass == null ? Map.of() : characterClass.getAttributeCurves();

        if (levelInfoMap.isEmpty() && attributeCurves.isEmpty()) {
            applyMovementSpeed(movementSpeedLevel, null, null);
            return;
        }

        double previousMaxHealth = player.getMaxHealth();
        double healthPercentage = previousMaxHealth > 0 ? player.getHealth() / previousMaxHealth : 1.0;

        // First, reset all attributes to their default values to prevent stacking
        Attributable defaultAttributeInstance = player.getType().getDefaultAttributes();
        for (Attribute attribute : Registry.ATTRIBUTE) {
            if (!Attribute.MOVEMENT_SPEED.equals(attribute) && player.getAttribute(attribute) != null) {
                double defaultBase = getPlayerDefaultBaseValue(defaultAttributeInstance, attribute);
                player.getAttribute(attribute).setBaseValue(defaultBase);
            }
        }

        // Calculate cumulative attribute bonuses up to the character's level
        Map<Attribute, Double> cumulativeAttributes = new HashMap<>();

        int maxLevelToCheck = Math.max(characterLevel, movementSpeedLevel);
        for (int level = 1; level <= maxLevelToCheck; level++) {
            LevelInfo levelInfo = levelInfoMap.get(level);
            if (levelInfo != null && levelInfo.getBaseAttributeBonus() != null) {
                for (Map.Entry<Attribute, Double> entry : levelInfo.getBaseAttributeBonus().entrySet()) {
                    Attribute attribute = entry.getKey();
                    int effectiveLevel = Attribute.MOVEMENT_SPEED.equals(attribute) ? movementSpeedLevel : characterLevel;
                    if (level > effectiveLevel) {
                        continue;
                    }
                    Double bonus = entry.getValue();
                    cumulativeAttributes.merge(attribute, bonus, Double::sum);
                }
            }
        }

        // A configured curve replaces the default base plus the discipline's level bonuses.
        attributeCurves.keySet().forEach(attribute -> cumulativeAttributes.putIfAbsent(attribute, 0.0));
        applyMovementSpeed(movementSpeedLevel, cumulativeAttributes.remove(Attribute.MOVEMENT_SPEED),
                attributeCurves.get(Attribute.MOVEMENT_SPEED));
        for (Map.Entry<Attribute, Double> entry : cumulativeAttributes.entrySet()) {
            Attribute attribute = entry.getKey();
            if (player.getAttribute(attribute) != null) {
                HClass.AttributeCurve curve = attributeCurves.get(attribute);
                double finalBaseValue = curve == null
                        ? getPlayerDefaultBaseValue(defaultAttributeInstance, attribute) + entry.getValue()
                        : curve.valueAt(characterLevel);
                player.getAttribute(attribute).setBaseValue(finalBaseValue);
                if (attribute == Attribute.STAT_ENERGY) {
                    int maxEnergy = Math.max(0, (int) Math.round(finalBaseValue));
                    player.setMaxEnergy(maxEnergy);
                    player.setEnergy(Math.clamp(player.getEnergy(), 0, maxEnergy));
                }
            }
        }
        player.setHealthScaled(true);
        player.setHealthScale(20.0);
        player.setHealth(Math.min(player.getMaxHealth(), Math.max(0.0, player.getMaxHealth() * healthPercentage)));
    }

    private void applyMovementSpeed(int level, Double bonus, HClass.AttributeCurve curve) {
        var movement = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (movement != null) {
            // Bukkit exposes double the player's walking-speed base. Leave sprint and effect modifiers intact.
            double speedBonus = bonus == null ? 0.0 : bonus;
            movement.setBaseValue(curve == null ? player.getWalkSpeed() / 2.0 + speedBonus : curve.valueAt(level));
        }
    }

    private double getPlayerDefaultBaseValue(Attributable defaultAttributes, Attribute attribute) {
        if (defaultAttributes.getAttribute(attribute) != null) {
            return defaultAttributes.getAttribute(attribute).getBaseValue();
        }
        return player.getAttribute(attribute).getDefaultValue();
    }

    public void setScaledPvPMode(boolean scaledPvPMode) {
        setScaledPvPMode(scaledPvPMode, LEVEL_FOR_SCALED_PVP);
    }

    public void setScaledPvPMode(boolean scaledPvPMode, int scaledPvPLevel) {
        setScaledLevelMode(scaledPvPMode, scaledPvPLevel);
    }

    public void setScaledLevelMode(boolean scaledLevelMode, int scaledLevel) {
        this.scaledLevelMode = scaledLevelMode;
        this.scaledLevel = Math.max(1, scaledLevel);
        if (isInCastMode) {
            int level = character.getLevel();
            if (scaledLevelMode) {
                level = this.scaledLevel;
            }
            setAttributesForLevel(level, character.getLevel());
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
        if (Component.empty().equals(positive) && Component.empty().equals(negative)) {
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

    private boolean isValidTranslation(Component component, String spellId, int lineIndex) {
        if (component == null) {
            return false;
        }
        Integer cachedMaxLine = VALID_DESCRIPTION_LINE_CACHE.get(spellId);
        if (cachedMaxLine != null) {
            return lineIndex < cachedMaxLine;
        }
        String rendered = PlainTextComponentSerializer.plainText().serialize(component);
        String expectedMissingKey = "hecate.spellbook.spell.description." + spellId + "." + lineIndex;

        boolean isValid = !rendered.equals(expectedMissingKey);

        if (!isValid) {
            VALID_DESCRIPTION_LINE_CACHE.put(spellId, lineIndex);
        }

        return isValid;
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
