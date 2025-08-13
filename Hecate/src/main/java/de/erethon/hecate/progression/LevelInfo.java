package de.erethon.hecate.progression;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import org.bukkit.attribute.Attribute;

import java.util.Map;

public record LevelInfo(int level, String messageTranslationKey, Map<Attribute, Double> baseAttributeBonus) {

    double bonus(Attribute attribute) {
        return baseAttributeBonus.get(attribute);
    }

    public Map<Attribute, Double> getBaseAttributeBonus() {
        return baseAttributeBonus;
    }

    TranslatableComponent translatable() {
        return Component.translatable(messageTranslationKey);
    }
}
