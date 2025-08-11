package de.erethon.hecate.ui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class PrecomputedHealthDisplay {

    // Yes, this is terrible. But I really don't want tAo do expensive component updates for every damage event.

    private static final MiniMessage mm = MiniMessage.miniMessage();
    private static final String fullColor = "<st><#f5301b>";
    private static final String emptyColor = "<st><#57524f>";
    private static final String bar = "  ";

    public static Component ZERO = mm.deserialize(emptyColor + bar.repeat(20));
    public static Component TEN = mm.deserialize(fullColor + bar.repeat(2) + emptyColor + bar.repeat(18));
    public static Component TWENTY = mm.deserialize(fullColor + bar.repeat(4) + emptyColor + bar.repeat(16));
    public static Component THIRTY = mm.deserialize(fullColor + bar.repeat(6) + emptyColor + bar.repeat(14));
    public static Component FORTY= mm.deserialize(fullColor + bar.repeat(8) + emptyColor + bar.repeat(12));
    public static Component FIFTY = mm.deserialize(fullColor + bar.repeat(10) + emptyColor + bar.repeat(10));
    public static Component SIXTY = mm.deserialize(fullColor + bar.repeat(12) + emptyColor + bar.repeat(8));
    public static Component SEVENTY = mm.deserialize(fullColor + bar.repeat(14) + emptyColor + bar.repeat(6));
    public static Component EIGHTY = mm.deserialize(fullColor + bar.repeat(16) + emptyColor + bar.repeat(4));
    public static Component NINETY = mm.deserialize(fullColor + bar.repeat(18) + emptyColor + bar.repeat(2));
    public static Component HUNDRED = mm.deserialize(fullColor + bar.repeat(20));

    private final Component component;

     PrecomputedHealthDisplay(Component component) {
        this.component = component;
    }


    public static Component getComponentAt(double healthPercentage) {
        int health = (int) Math.ceil(healthPercentage * 10);
        switch (health) {
            case 0 -> {
                return ZERO;
            }
            case 1 -> {
                return TEN;
            }
            case 2 -> {
                return TWENTY;
            }
            case 3 -> {
                return THIRTY;
            }
            case 4 -> {
                return FORTY;
            }
            case 5 -> {
                return FIFTY;
            }
            case 6 -> {
                return SIXTY;
            }
            case 7 -> {
                return SEVENTY;
            }
            case 8 -> {
                return EIGHTY;
            }
            case 9 -> {
                return NINETY;
            }
            case 10 -> {
                return HUNDRED;
            }
        }
        return null;
    }
}
