package de.erethon.hecate.ui;

import de.erethon.hecate.classes.Traitline;
import de.erethon.spellbook.spells.vfx.SkillVfx;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;

/** Compact action-bar health and energy bars backed by the Hecate pack font. */
public final class CombatHud {
    private static final int STEPS = 20;
    // Geometry from the combat HUD bitmap generator. The first 10 pixels hold the icon.
    private static final int GLYPH_WIDTH = 84;
    private static final int BAR_LEFT = 10;
    private static final int BAR_ADVANCE = GLYPH_WIDTH + 1;
    private static final TextColor VALUE_COLOR = TextColor.color(0xE0E0E0);
    private static final Key SPACE_FONT = Key.key("hecate:combat_space");
    private static final Component[] HEALTH_BARS = glyphs("hecate:combat_health", 0xE200);
    private static final Component[] ENERGY_BARS = glyphs("hecate:combat_energy", 0xE240);
    private static final Component GAP = Component.text("   ");

    private CombatHud() { }

    public static Component render(Player player, Traitline traitline) {
        double health = Math.max(0, player.getHealth());
        double maxHealth = Math.max(0, player.getMaxHealth());
        int energy = Math.max(0, player.getEnergy());
        int maxEnergy = Math.max(0, player.getMaxEnergy());
        int healthStep = step(health, maxHealth);
        int energyStep = step(energy, maxEnergy);
        TextColor energyColor = traitline.getEnergyColor() == null ? NamedTextColor.AQUA : traitline.getEnergyColor();

        if (!player.hasMetadata(SkillVfx.LOADED_METADATA)) {
            return Component.empty()
                    .append(textBar(healthStep, NamedTextColor.RED))
                    .append(Component.text(" " + amount(health) + "/" + amount(maxHealth), NamedTextColor.WHITE)
                            .shadowColor(ShadowColor.none()))
                    .append(GAP)
                    .append(textBar(energyStep, energyColor))
                    .append(Component.text(" " + energy + "/" + maxEnergy, NamedTextColor.WHITE));
        }

        return Component.empty()
                .append(centeredBar(HEALTH_BARS[healthStep], Component.text(amount(health) + "/" + amount(maxHealth), VALUE_COLOR)
                        .shadowColor(ShadowColor.none())))
                .append(GAP)
                .append(centeredBar(ENERGY_BARS[energyStep].color(energyColor), Component.text(energy + "/" + maxEnergy, VALUE_COLOR)));
    }

    private static Component centeredBar(Component bar, TextComponent value) {
        // Digits and slash advance by six pixels, including one pixel after the last glyph.
        int textAdvance = value.content().length() * 6;
        int textWidth = textAdvance - 1;
        int left = BAR_LEFT + Math.max(0, (GLYPH_WIDTH - BAR_LEFT - textWidth) / 2);
        return Component.empty().append(bar)
                .append(space(-BAR_ADVANCE + left))
                .append(value)
                .append(space(BAR_ADVANCE - left - textAdvance));
    }

    private static Component space(int pixels) {
        if (pixels == 0) return Component.empty();
        StringBuilder glyphs = new StringBuilder();
        int remaining = Math.abs(pixels);
        int first = pixels < 0 ? 0xE288 : 0xE280;
        for (int bit = 0; remaining != 0; bit++, remaining >>= 1) {
            if ((remaining & 1) != 0) glyphs.append((char) (first + bit));
        }
        return Component.text(glyphs.toString()).font(SPACE_FONT);
    }

    private static Component[] glyphs(String font, int firstCodepoint) {
        Component[] result = new Component[STEPS + 1];
        for (int index = 0; index < result.length; index++) {
            result[index] = Component.text(String.valueOf((char) (firstCodepoint + index)))
                    .font(Key.key(font));
        }
        return result;
    }

    private static Component textBar(int twentyStep, TextColor color) {
        int filled = (twentyStep + 1) / 2;
        return Component.text("■".repeat(filled), color)
                .append(Component.text("□".repeat(10 - filled), NamedTextColor.DARK_GRAY));
    }

    private static int step(double value, double maximum) {
        if (value <= 0 || maximum <= 0) return 0;
        return Math.clamp((int) Math.ceil(value / maximum * STEPS), 1, STEPS);
    }

    private static String amount(double value) {
        return Long.toString(Math.round(value));
    }
}
