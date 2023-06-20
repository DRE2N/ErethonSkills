package de.erethon.hecate.ui;

import de.erethon.papyrus.DamageType;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

public class DamageColor {

    public static TextColor getColorForDamageType(DamageType type) {
        switch (type) {
            case AIR -> {
                return TextColor.color(0x00FFFF);
            }
            case EARTH -> {
                return TextColor.color(0x00FF00);
            }
            case FIRE -> {
                return TextColor.color(0xFF7B00);
            }
            case MAGIC -> {
                return TextColor.color(0x800080);
            }
            case PHYSICAL -> {
                return TextColor.color(0xFF001C);
            }
            case WATER -> {
                return TextColor.color(0x0000FF);
            }
            case PURE -> {
                return NamedTextColor.GOLD;
            }
        }
        return NamedTextColor.WHITE;
    }
}
