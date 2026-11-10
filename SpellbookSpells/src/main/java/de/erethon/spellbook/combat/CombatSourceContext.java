package de.erethon.spellbook.combat;

import de.erethon.papyrus.combat.CombatContext;
import de.erethon.spellbook.api.SpellEffect;
import de.erethon.spellbook.api.SpellbookSpell;
import org.bukkit.entity.LivingEntity;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Carries semantic combat-source information through synchronous Bukkit damage
 * and healing events. Papyrus currently exposes the final damage and element,
 * but not which Spellbook effect caused it.
 */
public final class CombatSourceContext {

    private static final ThreadLocal<Deque<Source>> SOURCES = new ThreadLocal<>();

    private CombatSourceContext() {
    }

    public static void runEffect(SpellEffect effect, Runnable action) {
        String className = effect.getClass().getSimpleName();
        String name = className.endsWith("Effect")
                ? className.substring(0, className.length() - "Effect".length())
                : className;
        run(new Source("effect:" + className, humanize(name), Kind.EFFECT, effect.getCaster()), action);
    }

    public static void runSpell(SpellbookSpell spell, Runnable action) {
        if (spell == null) {
            action.run();
            return;
        }
        Runnable papyrusAwareAction = () -> run(
                new Source("spell:" + spell.getId(), humanize(spell.getId()), Kind.SPELL, spell.getCaster()),
                action
        );
        CombatContext.runSpell(spell, papyrusAwareAction);
    }

    public static void run(Source source, Runnable action) {
        Deque<Source> sources = SOURCES.get();
        if (sources == null) {
            sources = new ArrayDeque<>();
            SOURCES.set(sources);
        }
        sources.push(source);
        try {
            action.run();
        } finally {
            sources.pop();
            if (sources.isEmpty()) {
                SOURCES.remove();
            }
        }
    }

    public static Source current() {
        Deque<Source> sources = SOURCES.get();
        return sources == null ? null : sources.peek();
    }

    private static String humanize(String id) {
        String normalized = id.replace('\\', '/');
        int separator = normalized.lastIndexOf('/');
        String name = separator >= 0 ? normalized.substring(separator + 1) : normalized;
        name = name.replace('_', ' ').replace('-', ' ');
        return name.isBlank() ? "Effect" : Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    public enum Kind {
        SPELL,
        EFFECT,
        TRAIT
    }

    public record Source(String id, String displayName, Kind kind, LivingEntity actor) {
    }
}
