package de.erethon.hecate.ui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class PrecomputedChannelDisplay {
    private static final MiniMessage mm = MiniMessage.miniMessage();
    private static final String fullColor = "<yellow>";
    private static final String emptyColor = "<gray>";
    private static final String bar = "■";

    public static Component ZERO = mm.deserialize(emptyColor + bar.repeat(10));
    public static Component TEN = mm.deserialize(fullColor + bar.repeat(1) + emptyColor + bar.repeat(9));
    public static Component TWENTY = mm.deserialize(fullColor + bar.repeat(2) + emptyColor + bar.repeat(8));
    public static Component THIRTY = mm.deserialize(fullColor + bar.repeat(3) + emptyColor + bar.repeat(7));
    public static Component FORTY= mm.deserialize(fullColor + bar.repeat(4) + emptyColor + bar.repeat(6));
    public static Component FIFTY = mm.deserialize(fullColor + bar.repeat(5) + emptyColor + bar.repeat(5));
    public static Component SIXTY = mm.deserialize(fullColor + bar.repeat(6) + emptyColor + bar.repeat(4));
    public static Component SEVENTY = mm.deserialize(fullColor + bar.repeat(7) + emptyColor + bar.repeat(3));
    public static Component EIGHTY = mm.deserialize(fullColor + bar.repeat(8) + emptyColor + bar.repeat(2));
    public static Component NINETY = mm.deserialize(fullColor + bar.repeat(9) + emptyColor + bar.repeat(1));
    public static Component HUNDRED = mm.deserialize(fullColor + bar.repeat(10));

    PrecomputedChannelDisplay(Component component) {
    }


    public static Component getComponentAt(int current, int channelDuration) {
        double channelPercentage = (double) current / channelDuration;
        int health = (int) Math.ceil(channelPercentage * 10);
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
