package de.erethon.spellbook.utils;

import de.erethon.spellbook.api.SpellCaster;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Coordinates temporary Spellbook feedback with persistent combat HUD output. */
public final class SpellbookFeedback {

    private static final long DISPLAY_TIME_MILLIS = 2_000;
    private static final Map<UUID, Long> VISIBLE_UNTIL = new ConcurrentHashMap<>();

    private SpellbookFeedback() {
    }

    public static void send(SpellCaster caster, String message) {
        if (caster instanceof LivingEntity entity) {
            entity.sendParsedActionBar(message);
            VISIBLE_UNTIL.put(entity.getUniqueId(), System.currentTimeMillis() + DISPLAY_TIME_MILLIS);
        }
    }

    public static boolean isVisible(Player player) {
        Long visibleUntil = VISIBLE_UNTIL.get(player.getUniqueId());
        if (visibleUntil == null) {
            return false;
        }
        if (visibleUntil > System.currentTimeMillis()) {
            return true;
        }
        VISIBLE_UNTIL.remove(player.getUniqueId(), visibleUntil);
        return false;
    }
}
