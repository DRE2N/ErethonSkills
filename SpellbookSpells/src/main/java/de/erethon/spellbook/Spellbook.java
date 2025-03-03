package de.erethon.spellbook;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookAPI;
import de.erethon.spellbook.api.TraitData;
import de.erethon.spellbook.teams.TeamManager;
import de.erethon.spellbook.utils.PetLookup;
import de.slikey.effectlib.EffectManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class Spellbook {

    private static Spellbook instance;
    private final SpellbookAPI api;
    private final Plugin implementer;
    private final EffectManager effectManager;
    private final TeamManager teamManager;

    private final PetLookup petLookup;

    private boolean DEBUG = true;

    private static final Random random = new Random();

    private final Set<EffectData> ccEffects = new HashSet<>();

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
        teamManager = new TeamManager();
        petLookup = new PetLookup();
        ccEffects.add(api.getLibrary().getEffectByID("Stun"));
        ccEffects.add(api.getLibrary().getEffectByID("Fear"));
        ccEffects.add(api.getLibrary().getEffectByID("Slow"));
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

    public TeamManager getTeamManager() {
        return teamManager;
    }

    public PetLookup getPetLookup() {
        return petLookup;
    }

    /**
     * @return a Set of all EffectData for effects that are considered crowd control effects
     */
    public Set<EffectData> getCCEffects() {
        return ccEffects;
    }

    /** Toggles debug mode. In debug mode, teams are ignored and additional debug messages are printed to the console.
     * @param debug whether to enable debug mode
     */
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
            //MessageUtil.log("Caster: " + (caster == null ? "null" : caster.getName())  + " Target: " + (target == null ? "null" : target.getName()) + " Attribute: " + attribute.name() + " Multiplier: " + multiplier);
        }
        if (target instanceof Player) {
            return getScaledValue(data, caster, true, attribute, multiplier);
        }
        return getScaledValue(data, caster, false, attribute, multiplier);
    }

    private static double getScaledValue(YamlConfiguration data, LivingEntity caster, boolean pvp, Attribute attribute, double multiplier) {
        if (pvp) {
            if (!data.contains("coefficients.players." + attribute.getKey())) {
                return caster.getAttribute(attribute).getValue() * multiplier;
            }
        }
        else {
            if (!data.contains("coefficients.entities." + attribute.getKey())) {
                return caster.getAttribute(attribute).getValue() * multiplier;
            }
        }
        if (getInstance().DEBUG) {
            //MessageUtil.log("Scaled value for entity damage " + attribute.name() + ": " + caster.getAttribute(attribute).getValue() * data.getDouble("coefficients.entities." + attribute.name().toUpperCase(), 1.0));
        }
        return (caster.getAttribute(attribute).getValue() * data.getDouble("coefficients.players." + attribute.getKey(), 1.0)) * multiplier;
    }

    /**
     * @param damage initial damage value
     * @param entity the entity that dealt the damage
     * @param canCrit if this damage can crit
     * @return the damage value after applying crit chance & damage, as well as random variation.
     */
    public static double getVariedDamage(double damage, LivingEntity entity, boolean canCrit) {
        double maxVariance = damage / VARIANCE;
        maxVariance = Math.max(1, maxVariance);
        double variance = -maxVariance + random.nextDouble(maxVariance * 2);
        damage += variance;
        if (canCrit) {
            int crit = random.nextInt(101);
            if (crit < entity.getAttribute(Attribute.STAT_CRIT_CHANCE).getValue()) {
                damage = (float) (damage + entity.getAttribute(Attribute.STAT_CRIT_DAMAGE).getValue());
            }
        }
        if (getInstance().DEBUG) {
            //MessageUtil.log("Entity: " + entity.getName() + " | Initial damage: " + damage + " | Variance: " + variance + " | Crit: " + canCrit + " | Final damage: " + damage);
        }
        return damage;
    }

    public static double getVariedAttributeBasedDamage(YamlConfiguration data, LivingEntity caster, LivingEntity target, boolean canCrit, Attribute attribute) {
        double damage = getScaledValue(data, caster, target, attribute);
        return getVariedDamage(damage, caster, canCrit);
    }

    public static boolean canAttack(LivingEntity attacker, LivingEntity target) {
        if (getInstance().DEBUG) {
            return true;
        }
        if (target.isInvulnerable()) {
            return false;
        }
        return !getInstance().getTeamManager().isInSameTeam(attacker, target);
    }

    public static Color parseColor(String input) {
        if (input.startsWith("#")) {
            return Color.fromRGB(Integer.parseInt(input.substring(1)));
        } else {
            return Color.fromRGB(Integer.parseInt(input));
        }
    }

    public static SpellData getSpellData(String id) {
        return getInstance().getAPI().getLibrary().getSpellByID(id);
    }

    public static TraitData getTraitData(String id) {
        return getInstance().getAPI().getLibrary().getTraitByID(id);
    }

    public static EffectData getEffectData(String id) {
        return getInstance().getAPI().getLibrary().getEffectByID(id);
    }
}


