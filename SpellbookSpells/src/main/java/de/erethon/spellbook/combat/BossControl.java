package de.erethon.spellbook.combat;

import de.erethon.papyrus.combat.BossControlEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;

/** Sends authored control pressure to a boss encounter when one owns the target. */
public final class BossControl {

    private BossControl() {}

    /** @return true if the target is a boss whose encounter handles control. */
    public static boolean apply(LivingEntity actor, LivingEntity target, double pressure) {
        BossControlEvent event = new BossControlEvent(actor, target, pressure);
        Bukkit.getPluginManager().callEvent(event);
        return event.isHandled();
    }
}
