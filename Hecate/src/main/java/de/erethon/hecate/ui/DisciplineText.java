package de.erethon.hecate.ui;

import net.kyori.adventure.text.format.TextColor;

/** Colors and config-text cleanup for the discipline dialog. */
final class DisciplineText {
    static final TextColor LIGHT = TextColor.color(0xF0F3EF);
    static final TextColor GOLD = TextColor.color(0xE4C58D);
    static final TextColor MUTED = TextColor.color(0xC4CECB);
    static final TextColor LOCKED = TextColor.color(0xB3BCBA);

    private DisciplineText() { }

    static String plain(String source) {
        return source == null ? "" : source.replaceAll("<[^>]*>", "")
                .replaceAll("§[0-9a-fk-or]", "").replaceAll("\\s+", " ").trim();
    }
}
