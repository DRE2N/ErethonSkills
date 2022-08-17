package de.erethon.spellbook;

import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookAPI;
import de.slikey.effectlib.EffectManager;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public class Spellbook {

    private static Spellbook instance;
    private final SpellbookAPI api;
    private final Plugin implementer;
    private final EffectManager effectManager;

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

    public static double getScaledValue(YamlConfiguration data, LivingEntity entity, Attribute attribute) {
        return getScaledValue(data, entity, attribute, 1.0);
    }

    public static double getScaledValue(YamlConfiguration data, LivingEntity entity, Attribute attribute, double multiplier) {
        return (entity.getAttribute(attribute).getValue() * data.getDouble("coefficient-" + attribute.name(), 1.0)) * multiplier;
    }



}
