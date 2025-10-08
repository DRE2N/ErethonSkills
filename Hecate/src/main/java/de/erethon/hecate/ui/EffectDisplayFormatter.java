package de.erethon.hecate.ui;

import de.erethon.spellbook.api.SpellEffect;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.Collection;

/**
 * Utility class for formatting spell effects for display in UI components.
 */
public class EffectDisplayFormatter {

    /**
     * Formats a collection of spell effects into positive and negative components.
     * Effects with less than 20 ticks remaining are filtered out.
     *
     * @param effects The collection of spell effects to format
     * @return An array with two components: [0] = positive effects, [1] = negative effects
     */
    public static Component[] formatEffects(Collection<SpellEffect> effects) {
        Component positiveEffects = Component.empty();
        Component negativeEffects = Component.empty();

        for (SpellEffect effect : effects) {
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

        return new Component[]{positiveEffects, negativeEffects};
    }

    /**
     * Formats a single spell effect with icon, stack count, and duration.
     *
     * @param effect The spell effect to format
     * @return The formatted component
     */
    public static Component formatEffectDisplay(SpellEffect effect) {
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
            int padding = 3 - durationText.length();
            String paddedDuration = " ".repeat(Math.max(0, padding)) + durationText;

            display = display.append(Component.space()).append(Component.text(paddedDuration, NamedTextColor.GRAY));
        }
        return display;
    }

    /**
     * Combines positive and negative effect components with a spacer.
     *
     * @param positive The positive effects component
     * @param negative The negative effects component
     * @return The combined component, or empty if both are empty
     */
    public static Component combineEffects(Component positive, Component negative) {
        boolean hasPositive = positive != null && !positive.equals(Component.empty());
        boolean hasNegative = negative != null && !negative.equals(Component.empty());

        if (!hasPositive && !hasNegative) {
            return Component.empty();
        }

        Component spacer = Component.text(" | ", NamedTextColor.DARK_GRAY);
        Component result = Component.empty();

        if (hasPositive) {
            result = result.append(positive);
        }

        if (hasPositive && hasNegative) {
            result = result.append(spacer);
        }

        if (hasNegative) {
            result = result.append(negative);
        }

        return result;
    }
}
