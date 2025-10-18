package de.erethon.spellbook.spells;

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
    public int targetRaytraceSize = data.getInt("targetRaySize", 1);
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
        return super.onCast();
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
            caster.sendParsedActionBar(SpellbookCommonMessages.NO_TARGET);
            return false;
        }
        if (!(result.getHitEntity() instanceof LivingEntity)) {
            caster.sendParsedActionBar(SpellbookCommonMessages.NO_TARGET);
            return false;
        }
        this.target = (LivingEntity) result.getHitEntity();
        return true;
    }
}
