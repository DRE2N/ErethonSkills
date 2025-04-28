package de.erethon.hecate.util;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.chat.MiniMessageTranslator;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.util.TriState;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SpellbookTranslator extends MiniMessageTranslator {

    private final Map<String, Map<Locale, String>> translations = new HashMap<>();

    public void registerTranslation(String key, String translation, Locale locale) {
        translations.computeIfAbsent(key, k -> new HashMap<>()).put(locale, translation);
    }

    @Override
    public @NotNull Key name() {
        return Key.key("spellbook");
    }

    @Override
    public @NotNull TriState hasAnyTranslations() {
        return TriState.TRUE;
    }

    @Override
    protected @Nullable String getMiniMessageString(@NotNull String key, @NotNull Locale locale) {
        Map<Locale, String> map = translations.get(key);
        if (map == null) {
            return null;
        }
        if (map.get(locale) == null) { // fallback to US
            locale = Locale.US;
        }
        return map.get(locale);
    }
}
