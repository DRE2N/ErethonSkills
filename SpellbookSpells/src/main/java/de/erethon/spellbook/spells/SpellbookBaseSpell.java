package de.erethon.spellbook.spells;

import de.erethon.spellbook.utils.SpellbookFeedback;
import de.erethon.spellbook.spells.vfx.SkillSignatureVfx;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.animation.Animation;
import de.erethon.spellbook.aoe.AoE;
import de.erethon.spellbook.aoe.AoEParameters;
import de.erethon.spellbook.aoe.AoEShape;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookSpell;
import de.erethon.spellbook.utils.SpellbookCommonMessages;
import de.erethon.spellbook.utils.Targeted;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    protected Set<Animation> activeAnimations = new HashSet<>();

    public int range = data.getInt("range", 32);
    public double targetRaytraceSize = data.getDouble("targetRaySize", 1);
    public LivingEntity target;

    public SpellbookBaseSpell(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected void onTick() {
        // Tick all active animations
        activeAnimations.removeIf(animation -> {
            if (animation.isFinished()) {
                animation.stop(); // Ensure cleanup is called
                return true; // Remove finished animations
            }
            animation.tick();
            return false;
        });
    }

    @Override
    public boolean onCast() {
        boolean cast = super.onCast();
        if (cast) SkillSignatureVfx.cast(caster, target, getClass());
        return cast;
    }

    /**
     * Adds an animation to be ticked by this spell
     */
    protected void addAnimation(Animation animation) {
        activeAnimations.add(animation);
    }

    /**
     * Removes and stops an animation
     */
    protected void removeAnimation(Animation animation) {
        if (activeAnimations.remove(animation)) {
            animation.stop();
        }
    }

    /**
     * Cleans up all active animations
     */
    protected void cleanupAnimations() {
        for (Animation animation : activeAnimations) {
            animation.stop();
        }
        activeAnimations.clear();
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
        addPlayerCoefficientPlaceholders(caster);
        addTooltipValuePlaceholders(caster);
        return spellAddedPlaceholders;
    }

    private void addPlayerCoefficientPlaceholders(SpellCaster caster) {
        ConfigurationSection players = data.getConfigurationSection("coefficients.players");
        if (players == null) return;
        for (String key : players.getKeys(false)) {
            if (ATTRIBUTE_NAMES.contains(key)) {
                addPlayerCoefficient(caster, key, null);
                continue;
            }
            ConfigurationSection section = players.getConfigurationSection(key);
            if (section == null) continue;
            for (String attributeKey : section.getKeys(false)) {
                if (ATTRIBUTE_NAMES.contains(attributeKey)) {
                    addPlayerCoefficient(caster, attributeKey, key);
                }
            }
        }
    }

    private void addPlayerCoefficient(SpellCaster caster, String attributeKey, String section) {
        Attribute attribute = Registry.ATTRIBUTE.get(Key.key(attributeKey));
        if (attribute == null) return;
        String path = "coefficients.players." + (section == null ? "" : section + ".") + attributeKey;
        double value = ((LivingEntity) caster).getAttribute(attribute).getValue() * data.getDouble(path, 1.0);
        TextColor color = attribute == Attribute.ADVANTAGE_PHYSICAL ? ATTR_PHYSICAL_COLOR
                : attribute == Attribute.ADVANTAGE_MAGICAL ? ATTR_MAGIC_COLOR
                : attribute == Attribute.STAT_HEALINGPOWER ? ATTR_HEALING_POWER_COLOR : VALUE_COLOR;
        addTooltipValue(path, value, color);
    }

    private void addTooltipValuePlaceholders(SpellCaster caster) {
        LivingEntity living = (LivingEntity) caster;
        double physical = living.getAttribute(Attribute.ADVANTAGE_PHYSICAL).getValue();
        double magical = living.getAttribute(Attribute.ADVANTAGE_MAGICAL).getValue();
        double healingPower = Math.max(0, Math.min(
                data.getString("class", "").startsWith("assassin.") ? 80 : 100,
                living.getAttribute(Attribute.STAT_HEALINGPOWER).getValue()));
        double resistance = Math.min(100,
                Math.max(0, living.getAttribute(Attribute.RESISTANCE_PHYSICAL).getValue())
                        + Math.max(0, living.getAttribute(Attribute.RESISTANCE_MAGICAL).getValue()));
        addTooltipValue("tooltip.physical", physical, ATTR_PHYSICAL_COLOR);
        addTooltipValue("tooltip.magical", magical, ATTR_MAGIC_COLOR);
        addTooltipValue("tooltip.heal", data.getDouble("baseHealing")
                + data.getDouble("healingPowerCoefficient") * healingPower, ATTR_HEALING_POWER_COLOR);
        addTooltipValue("tooltip.castHeal", data.getDouble("castHealBase") + 0.3 * healingPower,
                ATTR_HEALING_POWER_COLOR);
        addTooltipValue("tooltip.hitHeal", data.getDouble("hitHealBase") + 0.1 * healingPower,
                ATTR_HEALING_POWER_COLOR);
        addTooltipValue("tooltip.shieldHeal", data.getDouble("healAmount") + 0.12 * healingPower,
                ATTR_HEALING_POWER_COLOR);
        addTooltipValue("tooltip.holyFireImmediate", 8 + 0.2 * healingPower,
                ATTR_HEALING_POWER_COLOR);
        addTooltipValue("tooltip.holyFireFinish", data.getDouble("baseFinishHeal") + 0.25 * healingPower,
                ATTR_HEALING_POWER_COLOR);
        addTooltipValue("tooltip.absorb", data.getDouble("baseAbsorb")
                + data.getDouble("resistanceCoefficient") * resistance, VALUE_COLOR);
        addTooltipValue("tooltip.guard", data.getDouble("baseGuard")
                + data.getDouble("resistanceCoefficient") * resistance, VALUE_COLOR);
        addTooltipValue("tooltip.prevented", data.getDouble("basePrevented")
                + data.getDouble("resistanceCoefficient") * resistance, VALUE_COLOR);
        addTooltipValue("tooltip.flankBonus", Math.min(data.getDouble("flankBonusMax"),
                data.getDouble("flankBonusMin") + Math.max(0, magical)
                        * data.getDouble("flankBonusMagicalCoefficient")), ATTR_MAGIC_COLOR);
        addTooltipValue("tooltip.shadowBuffer", Math.min(0.25,
                data.getDouble("baseBufferReduction")
                        + Math.max(0, Math.max(
                                living.getAttribute(Attribute.RESISTANCE_PHYSICAL).getValue(),
                                living.getAttribute(Attribute.RESISTANCE_MAGICAL).getValue()))
                        * data.getDouble("resistanceCoefficient")), VALUE_COLOR);
        addTooltipValue("tooltip.shieldBash", data.getDouble("counterDamage")
                + Math.min(100, Math.max(0, physical)) * 0.2, ATTR_PHYSICAL_COLOR);
        double physicalResistance = Math.max(0, living.getAttribute(Attribute.RESISTANCE_PHYSICAL).getValue());
        double magicalResistance = Math.max(0, living.getAttribute(Attribute.RESISTANCE_MAGICAL).getValue());
        addTooltipValue("tooltip.echoPhysicalGuard", Math.min(10, 2 + physicalResistance * 0.08), VALUE_COLOR);
        addTooltipValue("tooltip.echoMagicalGuard", Math.min(10, 2 + magicalResistance * 0.08), VALUE_COLOR);
        addTooltipValue("tooltip.launchPhysicalGuard", Math.min(8, 1.5 + physicalResistance * 0.06), VALUE_COLOR);
        addTooltipValue("tooltip.launchMagicalGuard", Math.min(8, 1.5 + magicalResistance * 0.06), VALUE_COLOR);
        addTooltipValue("tooltip.celestialMagicBonus", Math.min(20, Math.max(0, magical)) * 0.2,
                ATTR_MAGIC_COLOR);
    }

    private void addTooltipValue(String name, double value, TextColor color) {
        double rounded = Math.round(value * 100.0) / 100.0;
        String display = rounded == Math.rint(rounded)
                ? String.valueOf((long) rounded) : String.valueOf(rounded);
        spellAddedPlaceholders.add(Component.text(display, color));
        placeholderNames.add(name);
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

    /**
     * Creates an AoE with the specified parameters
     * @param center The center location of the AoE
     * @param shape The shape of the AoE
     * @param parameters The parameters defining the AoE's dimensions
     * @param rotation Optional rotation vector for oriented shapes
     * @param duration Duration in ticks (-1 for permanent)
     * @return The created AoE
     */
    protected AoE createAoE(Location center, AoEShape shape, AoEParameters parameters, Vector rotation, long duration) {
        return new AoE(this, caster, center, shape, parameters, rotation, duration);
    }

    /**
     * Creates a circular AoE
     * @param center The center location
     * @param radius The radius of the circle
     * @param height The height of the AoE
     * @param duration Duration in ticks (-1 for permanent)
     * @return The created AoE
     */
    protected AoE createCircularAoE(Location center, double radius, double height, long duration) {
        return createAoE(center, AoEShape.CIRCLE, AoEParameters.circle(radius, height), null, duration);
    }

    /**
     * Creates a rectangular AoE
     * @param center The center location
     * @param width The width of the rectangle
     * @param length The length of the rectangle
     * @param height The height of the AoE
     * @param rotation Optional rotation vector
     * @param duration Duration in ticks (-1 for permanent)
     * @return The created AoE
     */
    protected AoE createRectangularAoE(Location center, double width, double length, double height, Vector rotation, long duration) {
        return createAoE(center, AoEShape.RECTANGLE, AoEParameters.rectangle(width, length, height), rotation, duration);
    }

    /**
     * Creates a cone AoE
     * @param center The center location
     * @param radius The radius of the cone
     * @param angle The angle of the cone in degrees
     * @param height The height of the AoE
     * @param direction The direction the cone faces
     * @param duration Duration in ticks (-1 for permanent)
     * @return The created AoE
     */
    protected AoE createConeAoE(Location center, double radius, double angle, double height, Vector direction, long duration) {
        return createAoE(center, AoEShape.CONE, AoEParameters.cone(radius, angle, height), direction, duration);
    }

    protected boolean lookForTarget() {
        return lookForTarget(false, range);
    }

    protected boolean lookForTarget(int range) {
        return lookForTarget(false, range);
    }

    protected boolean lookForTarget(boolean friendly) {
        return lookForTarget(friendly, range);
    }

    protected boolean lookForTarget(boolean friendly, int range) {
        RayTraceResult result = caster.getWorld().rayTraceEntities(
            caster.getEyeLocation(),
            caster.getEyeLocation().getDirection(),
            range,
            targetRaytraceSize,
            entity -> {
                if (!(entity instanceof LivingEntity)) {
                    return false;
                }
                if (entity.equals(caster)) {
                    return false;
                }
                LivingEntity living = (LivingEntity) entity;
                // For friendly targeting, we want allies (cannot attack)
                // For hostile targeting, we want enemies (can attack)
                if (friendly) {
                    return !Spellbook.canAttack(caster, living);
                } else {
                    return Spellbook.canAttack(caster, living);
                }
            }
        );

        if (result == null || result.getHitEntity() == null) {
            SpellbookFeedback.send(caster, SpellbookCommonMessages.NO_TARGET);
            return false;
        }
        if (!(result.getHitEntity() instanceof LivingEntity)) {
            SpellbookFeedback.send(caster, SpellbookCommonMessages.NO_TARGET);
            return false;
        }
        this.target = (LivingEntity) result.getHitEntity();
        return true;
    }
}
