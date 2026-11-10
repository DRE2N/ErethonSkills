package de.erethon.spellbook.spells.assassin.shadow;

import org.bukkit.entity.LivingEntity;
import java.util.UUID;

/** Keeps one Shadow's dagger mark separate from another Shadow's setup. */
public final class ShadowMark {
    private ShadowMark() {}

    public static String tag(LivingEntity caster) {
        return "assassin.daggerthrow.marked." + caster.getUniqueId();
    }

    public static String apply(LivingEntity caster, LivingEntity target) {
        String owner = tag(caster);
        target.getTags().removeIf(tag -> tag.startsWith(owner + ".cast."));
        String token = owner + ".cast." + UUID.randomUUID().toString().substring(0, 12);
        target.getTags().add(owner);
        target.getTags().add(token);
        return token;
    }

    public static boolean isCurrent(LivingEntity target, String token) {
        return token != null && target.getTags().contains(token);
    }

    public static void clearIfCurrent(LivingEntity caster, LivingEntity target, String token) {
        if (!isCurrent(target, token)) return;
        target.getTags().remove(token);
        target.getTags().remove(tag(caster));
    }

    public static boolean consume(LivingEntity caster, LivingEntity target) {
        String owner = tag(caster);
        if (!target.getTags().remove(owner)) return false;
        target.getTags().removeIf(tag -> tag.startsWith(owner + ".cast."));
        return true;
    }
}
