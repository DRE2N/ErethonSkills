package de.erethon.spellbook;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookAPI;
import de.erethon.spellbook.api.TraitData;
import de.erethon.spellbook.teams.TeamManager;
import de.erethon.spellbook.utils.PetLookup;
import de.erethon.spellbook.utils.SpellbookCommonMessages;
import de.erethon.spellbook.utils.SpellbookTranslator;
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class Spellbook {

    private static Spellbook instance;
    private final SpellbookTranslator reg = new SpellbookTranslator();
    private final SpellbookAPI api;
    private final Plugin implementer;
    private final EffectManager effectManager;
    private final TeamManager teamManager;
    private final AoEManager aoeManager;

    private final PetLookup petLookup;

    private boolean DEBUG = true;

    private static final Random random = new Random();

    private final Set<EffectData> ccEffects = new HashSet<>();

    private final EffectData stability = Bukkit.getServer().getSpellbookAPI().getLibrary().getEffectByID("Stability");
    private final EffectData resistance = Bukkit.getServer().getSpellbookAPI().getLibrary().getEffectByID("Resistance");
    private final EffectData silence = Bukkit.getServer().getSpellbookAPI().getLibrary().getEffectByID("Silence");

    public static final HashMap<SpellData, Map<String, String>> PLACEHOLDER_CACHE = new HashMap<>();

    /**
     * Damage is divided by this value, and the result is the maximum variance.
     * Used to make damage values slightly less predictable.
     */
    private static final int VARIANCE = 10;

    public Spellbook(Plugin implementer) {
        instance = this;
        new SpellbookCommonMessages();
        this.implementer = implementer;
        this.api = Bukkit.getServer().getSpellbookAPI();
        effectManager = new EffectManager(implementer);
        teamManager = new TeamManager();
        petLookup = new PetLookup();
        aoeManager = new AoEManager();
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

    public AoEManager getAoEManager() {
        return aoeManager;
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

    public boolean isCCImmune(SpellCaster caster) {
        if (caster == null) {
            return false;
        }
        return caster.hasEffect(stability);
    }

    public boolean isSilenced(SpellCaster caster) {
        if (caster == null) {
            return false;
        }
        return caster.hasEffect(silence);
    }

    public boolean isResistant(SpellCaster caster) {
        if (caster == null) {
            return false;
        }
        return caster.hasEffect(resistance);
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
     * A value, limited to a range, based on the attribute of the caster.
     */
    public static double getRangedValue(YamlConfiguration data, LivingEntity caster, LivingEntity target, Attribute attribute, double min, double max) {
        double value = getScaledValue(data, caster, target, attribute);
        if (value < min) {
            value = min;
        }
        if (value > max) {
            value = max;
        }
        return value;
    }

    public static double getRangedValue(YamlConfiguration data, LivingEntity caster, LivingEntity target, Attribute attribute, double min, double max, String id) {
        double value = getScaledValue(data, caster, target, attribute, id);
        if (value < min) {
            value = min;
        }
        if (value > max) {
            value = max;
        }
        return value;
    }

    public static double getRangedValue(YamlConfiguration data, LivingEntity caster,Attribute attribute, double min, double max) {
        double value = getScaledValue(data, caster, attribute);
        if (value < min) {
            value = min;
        }
        if (value > max) {
            value = max;
        }
        return value;
    }

    public static double getRangedValue(YamlConfiguration data, LivingEntity caster,Attribute attribute, double min, double max, String id) {
        double value = getScaledValue(data, caster, attribute, id);
        if (value < min) {
            value = min;
        }
        if (value > max) {
            value = max;
        }
        return value;
    }

    /**
     * A value, limited to a range, based on the attribute of the caster.
     */
    public static double getRangedValue(YamlConfiguration data, LivingEntity caster, LivingEntity target, Attribute attribute, double min, double max, double attributeMultiplier) {
        double value = getScaledValue(data, caster, target, attribute, attributeMultiplier);
        if (value < min) {
            value = min;
        }
        if (value > max) {
            value = max;
        }
        return value;
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

    public static double getScaledValue(YamlConfiguration data, LivingEntity caster, Attribute attribute, String id) {
        return getScaledValue(data, caster, null, attribute, id);
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
            //Spellbook.log("Caster: " + (caster == null ? "null" : caster.getName())  + " Target: " + (target == null ? "null" : target.getName()) + " Attribute: " + attribute.getKey() + " Multiplier: " + multiplier);
        }
        if (target instanceof Player) {
            return getScaledValue(data, caster, true, attribute, multiplier);
        }
        return getScaledValue(data, caster, false, attribute, multiplier);
    }

    public static double getScaledValue(YamlConfiguration data, LivingEntity caster, LivingEntity target, Attribute attribute, String id) {
        if (getInstance().DEBUG) {
            //Spellbook.log("Caster: " + (caster == null ? "null" : caster.getName())  + " Target: " + (target == null ? "null" : target.getName()) + " Attribute: " + attribute.getKey() + " Multiplier: " + id);
        }
        if (target instanceof Player) {
            return getScaledValue(data, caster, true, attribute, id);
        }
        return getScaledValue(data, caster, false, attribute, id);
    }

    private static double getScaledValue(YamlConfiguration data, LivingEntity caster, boolean pvp, Attribute attribute, double multiplier) {
        if (pvp) {
            if (!data.contains("coefficients.players." + attribute.getKey())) {
                if (getInstance().DEBUG) {
                    Spellbook.log("Coefficients for " + attribute.getKey() + " not found. Using default values.");
                }
                return caster.getAttribute(attribute).getValue() * multiplier;
            }
        }
        else {
            if (!data.contains("coefficients.entities." + attribute.getKey())) {
                if (getInstance().DEBUG) {
                    Spellbook.log("Coefficients for " + attribute.getKey() + " not found. Using default values.");
                }
                return caster.getAttribute(attribute).getValue() * multiplier;
            }
        }
        if (getInstance().DEBUG) {
            //Spellbook.log("Scaled value for " + attribute.getKey() + ": " + caster.getAttribute(attribute).getValue() * data.getDouble("coefficients.entities." + attribute.getKey(), 1.0));
        }
        return (caster.getAttribute(attribute).getValue() * data.getDouble("coefficients.players." + attribute.getKey(), 1.0)) * multiplier;
    }

    private static double getScaledValue(YamlConfiguration data, LivingEntity caster, boolean pvp, Attribute attribute, String id) {
        if (pvp) {
            if (!data.contains("coefficients.players." + id + "." + attribute.getKey())) {
                if (getInstance().DEBUG) {
                    Spellbook.log("Coefficients for " + id + " for " + attribute.getKey() + " not found. Using default values.");
                }
                return getScaledValue(data, caster, true, attribute, 1.0);
            }
        }
        else {
            if (!data.contains("coefficients.entities." + id + "." + attribute.getKey())) {
                if (getInstance().DEBUG) {
                    Spellbook.log("Coefficients for " + id + " for " + attribute.getKey() + " not found. Using default values.");
                }
                return getScaledValue(data, caster, false, attribute, 1.0);
            }
        }
        if (getInstance().DEBUG) {
            //Spellbook.log("Scaled value for " + attribute.translationKey() + ": " + caster.getAttribute(attribute).getValue() * data.getDouble("coefficients.entities." + id + "." + attribute.getKey(), 1.0));
        }
        return (caster.getAttribute(attribute).getValue() * data.getDouble("coefficients.players." + id + "." + attribute.getKey(), 1.0));
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
            //Spellbook.log("Entity: " + entity.getName() + " | Initial damage: " + damage + " | Variance: " + variance + " | Crit: " + canCrit + " | Final damage: " + damage);
        }
        return damage;
    }

    public static double getVariedAttributeBasedDamage(YamlConfiguration data, LivingEntity caster, LivingEntity target, boolean canCrit, Attribute attribute) {
        double damage = getScaledValue(data, caster, target, attribute);
        return getVariedDamage(damage, caster, canCrit);
    }

    public static double getVariedAttributeBasedDamage(YamlConfiguration data, LivingEntity caster, LivingEntity target, boolean canCrit, Attribute attribute, String id) {
        double damage = getScaledValue(data, caster, target, attribute, id);
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
    
    public static void log(String message) {
        getInstance().getImplementer().getLogger().info("[Spellbook] " + message);
    }

    public SpellbookTranslator getTranslator() {
        return reg;
    }
}


