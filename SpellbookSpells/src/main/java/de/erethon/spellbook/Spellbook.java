package de.erethon.spellbook;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.spellbook.api.SpellbookAPI;
import de.slikey.effectlib.EffectManager;
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
        if (target instanceof Player) {
            if (!data.contains("coefficients.players" + attribute.name().toUpperCase())) {
                MessageUtil.log("Coefficient for player: " + attribute.name() + " not defined in " + data.getName());
                return caster.getAttribute(attribute).getValue() * multiplier;
            }
            return (caster.getAttribute(attribute).getValue() * data.getDouble("coefficients.players" + attribute.name().toUpperCase(), 1.0)) * multiplier;
        }
        if (!data.contains("coefficients.entities" + attribute.name().toUpperCase())) {
            MessageUtil.log("Coefficient for entities: " + attribute.name() + " not defined in " + data.getName());
            return caster.getAttribute(attribute).getValue() * multiplier;
        }
        return (caster.getAttribute(attribute).getValue() * data.getDouble("coefficients.entities" + attribute.name().toUpperCase(), 1.0)) * multiplier;
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
        return damage;
    }

    public static double getVariedAttributeBasedDamage(YamlConfiguration data, LivingEntity caster, LivingEntity target, boolean canCrit, Attribute attribute) {
        double damage = getScaledValue(data, caster, target, attribute);
        return getVariedDamage(damage, caster, canCrit);
    }



}
