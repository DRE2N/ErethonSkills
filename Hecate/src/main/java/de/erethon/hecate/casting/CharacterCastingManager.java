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
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellEffect;
import de.erethon.spellbook.api.SpellbookSpell;
import de.erethon.spellbook.api.TraitData;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import net.kyori.adventure.bossbar.BossBar;
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
    private boolean isInCastMode = false;
    private ItemStack castingItem;
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
        int health = (int) player.getHealth();
        int maxHealth = (int) player.getMaxHealth();

        Component healthIcon = Component.text("\u2665", NamedTextColor.RED);
        Component healthNumbers = Component.text(health + "/" + maxHealth, NamedTextColor.RED);
        Component healthText = healthIcon.append(Component.space()).append(healthNumbers);

        Traitline traitline = character.getTraitline();
        if (traitline == null) {
            player.sendParsedActionBar("<red>Internal error: No traitline found.");
            return;
        }
        TextColor energyColor = traitline.getEnergyColor();
        String energyIconUnicode = traitline.getEnergySymbol();
        if (energyIconUnicode == null || energyIconUnicode.isBlank()) {
            energyIconUnicode = "\u26A1"; // Default to lightning bolt
        }
        Component energyIcon = Component.text(energyIconUnicode, energyColor);
        Component energyNumbers = Component.text(energy + "/" + maxEnergy, energyColor);
        Component energyText = energyIcon.append(Component.space()).append(energyNumbers);

        Component positiveEffects = Component.empty();
        Component negativeEffects = Component.empty();

        for (SpellEffect effect : player.getEffects()) {
            if (effect.getTicksLeft() <= 20) {
                continue;
            }

            Component effectDisplay = formatEffectDisplay(effect);

            if (effect.data.isPositive()) {
                if (positiveEffects.equals(Component.empty())) {
                    positiveEffects = effectDisplay;
                } else {
                    positiveEffects = positiveEffects.append(Component.space()).append(effectDisplay);
                }
            } else {
                if (negativeEffects.equals(Component.empty())) {
                    negativeEffects = effectDisplay;
                } else {
                    negativeEffects = negativeEffects.append(Component.space()).append(effectDisplay);
                }
            }
        }
        updateBossbarUI(positiveEffects, negativeEffects);

        Component betweenSpacer = Component.text("     ");
        Component actionbar = healthText
                .append(betweenSpacer)
                .append(energyText);
        actionbar = actionbar.shadowColor(ShadowColor.none());
        player.sendActionBar(actionbar);
    }

    private Component formatEffectDisplay(SpellEffect effect) {
        Component display = Component.empty();
        Component icon = MiniMessage.miniMessage().deserialize(effect.data.getIcon());

        NamedTextColor effectColor = effect.data.isPositive() ? NamedTextColor.GREEN : NamedTextColor.RED;
        icon = icon.color(effectColor);

        int stacks = effect.getStacks();
        if (stacks > 1) {
            display = display.append(icon).append(Component.text("x" + stacks, NamedTextColor.YELLOW));
        } else {
            display = display.append(icon);
        }

        int duration = effect.getTicksLeft() / 20;
        if (duration > 0) {
            String durationText = duration + "s";
            StringBuilder paddedDuration = new StringBuilder();
            int padding = 3 - durationText.length();
            for (int i = 0; i < padding; i++) {
                paddedDuration.append(" ");
            }
            paddedDuration.append(durationText);

            display = display.append(Component.space()).append(Component.text(paddedDuration.toString(), NamedTextColor.GRAY));
        }
        return display;
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
        ItemStack emptyItem = new ItemStack(CastingStatics.SLOT_DYES[8]);
        emptyItem.setData(DataComponentTypes.ITEM_MODEL, Key.key("minecraft:air"));
        emptyItem.setData(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay().hideTooltip(true).build());
        return  emptyItem;
    }

    private ItemStack getItemStackFromSpellData(int slot, SpellData spellData) {
        ItemStack item = new ItemStack(CastingStatics.SLOT_DYES[slot]);
        Component name = Component.translatable("hecate.spellbook.spell.name." + spellData.getId());
        name = name.decoration(TextDecoration.ITALIC, false);
        item.setData(DataComponentTypes.CUSTOM_NAME, name);
        List<Component> placeholders = spellData.getActiveSpell(player).getPlaceholders(player);
        List<Component> lore = new ArrayList<>();
        for (int i = 0; i < spellData.getDescriptionLineCount(); i++) {
            lore.add(Component.translatable("hecate.spellbook.spell.description." + spellData.getId() + "." + i, placeholders));
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
            lore.add(Component.translatable("hecate.spellbook.spell.description." + spell.getId() + "." + i, spell.getPlaceholders(player)));
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

        // Try to preserve the original component's color if possible
        return Component.text(truncated.toString(), NamedTextColor.WHITE);
    }



}
