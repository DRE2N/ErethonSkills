package de.erethon.hecate.ui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.ShadowColor;

public final class PrecomputedHealthDisplay {

    private static final int STEPS = 20;
    private static final Component[] BARS = buildBars();

    private PrecomputedHealthDisplay() { }

    public static Component getComponentAt(double healthPercentage) {
        int step = Math.clamp((int) Math.ceil(healthPercentage * STEPS), 0, STEPS);
        return BARS[step];
    }

    private static Component[] buildBars() {
        Component[] bars = new Component[STEPS + 1];
        for (int step = 0; step <= STEPS; step++) {
            bars[step] = Component.translatable("hecate.combat.mob_health." + step, "")
                    .shadowColor(ShadowColor.none());
        }
        return bars;
    }
}
