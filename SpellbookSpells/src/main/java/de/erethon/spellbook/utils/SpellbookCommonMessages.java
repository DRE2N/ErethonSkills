package de.erethon.spellbook.utils;

import de.erethon.spellbook.Spellbook;

import java.util.Locale;

public class SpellbookCommonMessages {

    private final SpellbookTranslator translator = Spellbook.getInstance().getTranslator();

    public static final String NO_TARGET = "<lang:spellbook.casting.no_target>";
    public static final String NO_TARGET_BLOCK = "<lang:spellbook.casting.no_target_block>";
    public static final String TARGET_TOO_FAR = "<lang:spellbook.casting.target_too_far>";
    public static final String NOT_ENEMY = "<lang:spellbook.casting.not_enemy>";
    public static final String NOT_FRIENDLY = "<lang:spellbook.casting.not_friendly>";
    public static final String NO_ENERGY = "<lang:spellbook.casting.no_energy>";
    public static final String ON_COOLDOWN = "<lang:spellbook.casting.on_cooldown>";

    public SpellbookCommonMessages() {
        translator.registerTranslation("spellbook.casting.no_target", "<color:#ff0000>Kein Ziel ausgewählt!", Locale.GERMANY);
        translator.registerTranslation("spellbook.casting.no_target", "<color:#ff0000>No target selected!", Locale.US);
        translator.registerTranslation("spellbook.casting.no_target_block", "<color:#ff0000>Kein Block in Reichweite!", Locale.GERMANY);
        translator.registerTranslation("spellbook.casting.no_target_block", "<color:#ff0000>No block in range!", Locale.US);
        translator.registerTranslation("spellbook.casting.target_too_far", "<color:#ff0000>Ziel zu weit entfernt!", Locale.GERMANY);
        translator.registerTranslation("spellbook.casting.target_too_far", "<color:#ff0000>Target too far away!", Locale.US);
        translator.registerTranslation("spellbook.casting.not_enemy", "<color:#ff0000>Das Ziel ist kein Feind!", Locale.GERMANY);
        translator.registerTranslation("spellbook.casting.not_enemy", "<color:#ff0000>The target is not an enemy!", Locale.US);
        translator.registerTranslation("spellbook.casting.not_friendly", "<color:#ff0000>Das Ziel ist kein Verbündeter!", Locale.GERMANY);
        translator.registerTranslation("spellbook.casting.not_friendly", "<color:#ff0000>The target is not a friendly!", Locale.US);
        translator.registerTranslation("spellbook.casting.no_energy", "<color:#ff0000>Nicht genug Energie!", Locale.GERMANY);
        translator.registerTranslation("spellbook.casting.no_energy", "<color:#ff0000>Not enough energy!", Locale.US);
        translator.registerTranslation("spellbook.casting.on_cooldown", "<color:#ff0000>Noch auf Cooldown!", Locale.GERMANY);
        translator.registerTranslation("spellbook.casting.on_cooldown", "<color:#ff0000>Still on cooldown!", Locale.US);
    }
}
