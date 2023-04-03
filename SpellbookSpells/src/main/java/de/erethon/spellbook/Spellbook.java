package de.erethon.spellbook;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.spellbook.api.SpellbookAPI;
import de.slikey.effectlib.EffectManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Random;

public class Spellbook {

    private static Spellbook instance;
    private final SpellbookAPI api;
    private final Plugin implementer;
    private final EffectManager effectManager;

    private boolean DEBUG = false;

    private static final Random random = new Random();

    /**
     * Damage is divided by this value, and the result is the maximum variance.
     * Used to make damage values slightly less predictable.
     */
    private static final int VARIANCE = 10;

    public Spellbook(Plugin implementer) {
        instance = this;
        this.implementer = implementer;
        this.api = Bukkit.getServer().getSpellbookAPI();
        effectManager = new EffectManager(implementer);
    }

    public Plugin getImplementer() {
        return implementer;
    }

    public SpellbookAPI getAPI() {
        return api;
    }

    public static Spellbook getInstance() {
        return instance;
    }

    public EffectManager getEffectManager() {
        return effectManager;
    }

    public void setDebug(boolean debug) {
        this.DEBUG = debug;
    }

    public boolean isDebug() {
        return DEBUG;
    }

    /**
     * Get the scaled value of an attribute.
     * <b>Only use this method if the spell can not have a target.</b>
     * @param data the SpellData file
     * @param caster the entity that cast the spell
     * @param attribute the attribute
     * @return the value of the attribute after applying the coefficients defined in the SpellData file
     *
     */
    public static double getScaledValue(YamlConfiguration data, LivingEntity caster, Attribute attribute) {
        return getScaledValue(data, caster, null, attribute, 1.0);
    }

    /**
     * Get the scaled value of an attribute.
     * @param data the SpellData file
     * @param caster the entity that cast the spell
     * @param target the entity that is the target of the spell
     * @param attribute the attribute
     * @return the value of the attribute after applying the coefficients defined in the SpellData file
     */
    public static double getScaledValue(YamlConfiguration data, LivingEntity caster, LivingEntity target, Attribute attribute) {
        return getScaledValue(data, caster, target, attribute, 1.0);
    }

    /**
     * Get the scaled value of an attribute.
     * @param data the SpellData file
     * @param caster the entity that cast the spell
     * @param target the entity that is the target of the spell
     * @param attribute the attribute
     * @param multiplier an optional multiplier of the result
     * @return the value of the attribute after applying the coefficients defined in the SpellData file and the multiplier
     */
    public static double getScaledValue(YamlConfiguration data, LivingEntity caster, LivingEntity target, Attribute attribute, double multiplier) {
        if (getInstance().DEBUG) {
            MessageUtil.log("Caster: " + caster.getName() + " Target: " + (target == null ? "null" : target.getName()) + " Attribute: " + attribute.name() + " Multiplier: " + multiplier);
        }
        if (target instanceof Player) {
            return getScaledValue(data, caster, true, attribute, multiplier);
        }
        return getScaledValue(data, caster, false, attribute, multiplier);
    }

    private static double getScaledValue(YamlConfiguration data, LivingEntity caster, boolean pvp, Attribute attribute, double multiplier) {
        if (pvp) {
            if (!data.contains("coefficients.players." + attribute.name().toLowerCase())) {
                return caster.getAttribute(attribute).getValue() * multiplier;
            }
        }
        else {
            if (!data.contains("coefficients.entities." + attribute.name().toLowerCase())) {
                return caster.getAttribute(attribute).getValue() * multiplier;
            }
        }
        if (getInstance().DEBUG) {
            MessageUtil.log("Scaled value for entity damage " + attribute.name() + ": " + caster.getAttribute(attribute).getValue() * data.getDouble("coefficients.entities." + attribute.name().toUpperCase(), 1.0));
        }
        return (caster.getAttribute(attribute).getValue() * data.getDouble("coefficients.players." + attribute.name().toUpperCase(), 1.0)) * multiplier;
    }

    /**
     * @param damage initial damage value
     * @param entity the entity that dealt the damage
     * @param canCrit if this damage can crit
     * @return the damage value after applying crit chance & damage, as well as random variation.
     */
    public static double getVariedDamage(double damage, LivingEntity entity, boolean canCrit) {
        double maxVariance = damage / VARIANCE;
        double variance = -maxVariance + random.nextDouble(maxVariance * 2);
        damage += variance;
        if (canCrit) {
            int crit = random.nextInt(101);
            if (crit < entity.getAttribute(Attribute.STAT_CRIT_CHANCE).getValue()) {
                damage = (float) (damage + entity.getAttribute(Attribute.STAT_CRIT_DMG).getValue());
            }
        }
        if (getInstance().DEBUG) {
            MessageUtil.log("Entity: " + entity.getName() + " | Initial damage: " + damage + " | Variance: " + variance + " | Crit: " + canCrit + " | Final damage: " + damage);
        }
        return damage;
    }

    public static double getVariedAttributeBasedDamage(YamlConfiguration data, LivingEntity caster, LivingEntity target, boolean canCrit, Attribute attribute) {
        double damage = getScaledValue(data, caster, target, attribute);
        return getVariedDamage(damage, caster, canCrit);
    }

    public static Component replacePlaceholders(Component component, LivingEntity caster, YamlConfiguration data, boolean pvp, double multiplier) {
        TextReplacementConfig cd = TextReplacementConfig.builder().matchLiteral("%skill_cooldown%").replacement(Component.text(data.getInt("cooldown", 0)).color(TextColor.color(2, 125,202))).build();
        TextReplacementConfig range = TextReplacementConfig.builder().matchLiteral("%skill_range%").replacement(Component.text(data.getInt("range", 0)).color(TextColor.color(2, 125,202))).build();
        TextReplacementConfig duration = TextReplacementConfig.builder().matchLiteral("%skill_duration%").replacement(Component.text(data.getInt("duration", 0)).color(TextColor.color(2, 125,202))).build();
        TextReplacementConfig radius = TextReplacementConfig.builder().matchLiteral("%skill_radius%").replacement(Component.text(data.getInt("radius", 0)).color(TextColor.color(2, 125,202))).build();
        TextReplacementConfig energy = TextReplacementConfig.builder().matchLiteral("%skill_energy%").replacement(Component.text(data.getInt("energyCost", 0)).color(TextColor.color(2, 125,202))).build();
        if (data.contains("coefficients")) {
            TextReplacementConfig phys = TextReplacementConfig.builder().matchLiteral("%attribute_adv_physical%").replacement(Component.text(getScaledValue(data, caster, pvp, Attribute.ADV_PHYSICAL, multiplier)).color(TextColor.color(255, 0, 0))).build();
            TextReplacementConfig magic = TextReplacementConfig.builder().matchLiteral("%attribute_adv_magic%").replacement(Component.text(getScaledValue(data, caster, pvp, Attribute.ADV_PHYSICAL, multiplier)).color(TextColor.color(92, 14, 176))).build();
            TextReplacementConfig fire = TextReplacementConfig.builder().matchLiteral("%attribute_adv_fire%").replacement(Component.text(getScaledValue(data, caster, pvp, Attribute.ADV_PHYSICAL, multiplier)).color(TextColor.color(255, 180, 0))).build();
            TextReplacementConfig water = TextReplacementConfig.builder().matchLiteral("%attribute_adv_water%").replacement(Component.text(getScaledValue(data, caster, pvp, Attribute.ADV_PHYSICAL, multiplier)).color(TextColor.color(0, 30, 170))).build();
            TextReplacementConfig earth = TextReplacementConfig.builder().matchLiteral("%attribute_adv_earth%").replacement(Component.text(getScaledValue(data, caster, pvp, Attribute.ADV_PHYSICAL, multiplier)).color(TextColor.color(150, 100, 20))).build();
            TextReplacementConfig air = TextReplacementConfig.builder().matchLiteral("%attribute_adv_air%").replacement(Component.text(getScaledValue(data, caster, pvp, Attribute.ADV_PHYSICAL, multiplier)).color(TextColor.color(15, 200, 220))).build();
            TextReplacementConfig health = TextReplacementConfig.builder().matchLiteral("%attribute_health%").replacement(Component.text(getScaledValue(data, caster, pvp, Attribute.GENERIC_MAX_HEALTH, multiplier)).color(TextColor.color(255, 0, 20))).build();
            TextReplacementConfig atkspd = TextReplacementConfig.builder().matchLiteral("%attribute_attack_speed%").replacement(Component.text(getScaledValue(data, caster, pvp, Attribute.GENERIC_ATTACK_SPEED, multiplier)).color(TextColor.color(255, 255, 50))).build();
            TextReplacementConfig dmg = TextReplacementConfig.builder().matchLiteral("%attribute_attack_damage%").replacement(Component.text(getScaledValue(data, caster, pvp, Attribute.GENERIC_ATTACK_DAMAGE, multiplier)).color(TextColor.color(255, 255, 255))).build();
            TextReplacementConfig armor = TextReplacementConfig.builder().matchLiteral("%attribute_armor%").replacement(Component.text(getScaledValue(data, caster, pvp, Attribute.GENERIC_ARMOR, multiplier)).color(TextColor.color(122, 122, 122))).build();
            return component.replaceText(phys).replaceText(magic).replaceText(fire).replaceText(water).replaceText(earth).replaceText(air).replaceText(health).replaceText(atkspd).replaceText(dmg).replaceText(armor).replaceText(cd).replaceText(range).replaceText(duration).replaceText(radius).replaceText(energy);
        }
        return component.replaceText(cd).replaceText(range).replaceText(duration).replaceText(radius).replaceText(energy);
    }
}


