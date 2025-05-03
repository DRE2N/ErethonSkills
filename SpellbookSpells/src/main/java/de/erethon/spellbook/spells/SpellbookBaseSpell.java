package de.erethon.spellbook.spells;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookSpell;
import de.erethon.spellbook.utils.Targeted;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class SpellbookBaseSpell extends SpellbookSpell implements Targeted {

    public static final int CD_PLACEHOLDER = 0;
    public static TextColor VALUE_COLOR = TextColor.fromCSSHexString("#027DCA");
    public static TextColor ATTR_PHYSICAL_COLOR = TextColor.fromCSSHexString("#f02607");
    public static TextColor ATTR_MAGIC_COLOR = TextColor.fromCSSHexString("#0fdcfa");
    public static TextColor ATTR_HEALING_POWER_COLOR = TextColor.fromCSSHexString("#32ac21");
    public static TextColor ATTR_AIR_COLOR = TextColor.fromCSSHexString("#f0f0f0");

    private static final List<String> ATTRIBUTE_NAMES = new ArrayList<>(List.of(
            "minecraft:advantage_physical",
            "minecraft:advantage_magical",
            "minecraft:resistance_physical",
            "minecraft:resistance_magical",
            "minecraft:penetration_physical",
            "minecraft:penetration_magical",
            "minecraft:stat_healing_power",
            "minecraft:stat_crit_chance",
            "minecraft:stat_crit_damage"

    ));

    private static final List<String> IGNORED_KEYS = new ArrayList<>(List.of(
            "class",
            "name",
            "description",
            "availablePlaceholders",
            "icon"
    ));

    protected List<Component> spellAddedPlaceholders = new ArrayList<>();
    protected List<String> placeholderNames = new ArrayList<>();

    public SpellbookBaseSpell(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    public boolean onCast() {
        return super.onCast();
    }

    protected void addSpellPlaceholders() {

    }

    @Override
    public List<Component> getPlaceholders(SpellCaster caster) {
        spellAddedPlaceholders.clear();
        placeholderNames.clear();
        addBaseDataPlaceholdersOrdered();
        getCoefficientValuePlaceholders(caster);
        addSpellPlaceholders();
        return spellAddedPlaceholders;
    }

    protected void addBaseDataPlaceholdersOrdered() {
        Map<String, String> toCache = null;
        boolean cacheMiss = !Spellbook.PLACEHOLDER_CACHE.containsKey(data);
        if (cacheMiss) {
            toCache = new HashMap<>();
        }
        for (String key : data.getKeys(false)) {
            if (IGNORED_KEYS.contains(key) || key.equals("coefficients")) {
                continue;
            }
            placeholderNames.add(key);
            String value;
            if (!cacheMiss) {
                Map<String, String> cached = Spellbook.PLACEHOLDER_CACHE.get(data);
                value = cached.get(key);

                if (value == null) {
                    Object rawValue = data.get(key);
                    value = (rawValue != null) ? String.valueOf(rawValue) : "N/A";
                }
            } else {
                Object rawValue = data.get(key);
                value = (rawValue != null) ? String.valueOf(rawValue) : "N/A";
                toCache.put(key, value);
            }

            spellAddedPlaceholders.add(Component.text(value, VALUE_COLOR));
        }
        if (cacheMiss) {
            Spellbook.PLACEHOLDER_CACHE.put(data, toCache);
        }
    }

    protected void getCoefficientValuePlaceholders(SpellCaster caster) {
        ConfigurationSection topSection = data.getConfigurationSection("coefficients");
        if (topSection == null) {
            return;
        }
        ConfigurationSection coefficients = topSection.getConfigurationSection("entities"); // TODO: default to entities for now
        if (coefficients == null) {
            return;
        }
        for (String key : coefficients.getKeys(false)) {
            if (ATTRIBUTE_NAMES.contains(key)) { // We have a direct attribute key, not a named section
                Attribute attribute = Registry.ATTRIBUTE.get(Key.key(key));
                if (attribute == null) {
                    continue;
                }
                double value = Spellbook.getScaledValue(data, (LivingEntity) caster, attribute);
                TextColor color = VALUE_COLOR;
                if (attribute == Attribute.ADVANTAGE_PHYSICAL) {
                    color = ATTR_PHYSICAL_COLOR;
                } else if (attribute == Attribute.ADVANTAGE_MAGICAL) {
                    color = ATTR_MAGIC_COLOR;
                } else if (attribute == Attribute.STAT_HEALINGPOWER) {
                    color = ATTR_HEALING_POWER_COLOR;
                }
                spellAddedPlaceholders.add(Component.text(value, color));
                placeholderNames.add("coefficients.entities." + key);
                continue;
            }
            ConfigurationSection section = coefficients.getConfigurationSection(key); // We got a named section
            if (section == null) {
                continue;
            }
            for (String subKey : section.getKeys(false)) {
                if (ATTRIBUTE_NAMES.contains(subKey)) {
                    Attribute attribute = Registry.ATTRIBUTE.get(Key.key(subKey));
                    if (attribute == null) {
                        continue;
                    }
                    TextColor color = VALUE_COLOR;
                    if (attribute == Attribute.ADVANTAGE_PHYSICAL) {
                        color = ATTR_PHYSICAL_COLOR;
                    } else if (attribute == Attribute.ADVANTAGE_MAGICAL) {
                        color = ATTR_MAGIC_COLOR;
                    } else if (attribute == Attribute.STAT_HEALINGPOWER) {
                        color = ATTR_HEALING_POWER_COLOR;
                    }
                    double value = Spellbook.getScaledValue(data, (LivingEntity) caster, attribute, key); // Get the value from the section, not the overall attribute
                    spellAddedPlaceholders.add(Component.text(value, color));
                    placeholderNames.add("coefficients.entities." + key + "." + subKey);
                }
            }
        }
    }

    public List<String> getPlaceholderNames() {
        return placeholderNames;
    }
}
