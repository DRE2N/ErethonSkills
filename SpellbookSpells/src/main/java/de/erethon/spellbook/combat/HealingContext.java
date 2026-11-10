package de.erethon.spellbook.combat;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.persistence.PersistentDataType;

/** Records recent player combat for targetless and ally healing. */
public final class HealingContext implements Listener {

    private static final NamespacedKey LAST_PLAYER_COMBAT = new NamespacedKey("spellbook", "last_player_combat_at");
    private static final long PLAYER_COMBAT_MILLIS = 10_000L;

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim) || event.getFinalDamage() <= 0) return;
        Player attacker = findPlayer(event.getDamager());
        if (attacker == null || attacker == victim) {
            CombatSourceContext.Source source = CombatSourceContext.current();
            attacker = source == null || !(source.actor() instanceof Player player) ? null : player;
        }
        if (attacker == null || attacker == victim) return;
        long now = System.currentTimeMillis();
        attacker.getPersistentDataContainer().set(LAST_PLAYER_COMBAT, PersistentDataType.LONG, now);
        victim.getPersistentDataContainer().set(LAST_PLAYER_COMBAT, PersistentDataType.LONG, now);
    }

    public static boolean isPlayerCombat(LivingEntity entity) {
        if (!(entity instanceof Player player)) return false;
        long lastCombat = player.getPersistentDataContainer().getOrDefault(
                LAST_PLAYER_COMBAT, PersistentDataType.LONG, 0L);
        return System.currentTimeMillis() - lastCombat < PLAYER_COMBAT_MILLIS;
    }

    public static boolean isPlayerCombat(LivingEntity caster, LivingEntity healed) {
        return isPlayerCombat(caster) || isPlayerCombat(healed);
    }

    private Player findPlayer(Entity damager) {
        if (damager instanceof Player player) return player;
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player player) return player;
        return null;
    }
}
