package de.erethon.hecate.combatlog;

import de.erethon.hecate.Hecate;
import de.erethon.hecate.ui.DamageColor;
import de.erethon.papyrus.PDamageType;
import de.erethon.papyrus.combat.CombatDamageBreakdown;
import de.erethon.papyrus.combat.CombatSource;
import de.erethon.spellbook.combat.CombatSourceContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.projectiles.ProjectileSource;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class CombatLogManager implements Listener {

    private static final long QUIET_FLUSH_MILLIS = 1_500;
    private static final long MAX_BATCH_MILLIS = 5_000;
    private static final String GLOBAL_PERMISSION = "hecate.combatlog.all";

    private final NamespacedKey modeKey;
    private final Map<UUID, LinkedHashMap<BatchKey, Batch>> pending = new HashMap<>();
    private final int flushTask;

    public CombatLogManager(Hecate plugin) {
        modeKey = new NamespacedKey(plugin, "combat_log_mode");
        flushTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::flushExpired, 10, 10);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity target)) {
            return;
        }
        CombatDamageBreakdown advancedBreakdown = event instanceof EntityDamageByEntityEvent byEntity
                ? byEntity.getCombatBreakdown()
                : null;
        double rawDamage = Math.max(0, event.getFinalDamage());
        if (rawDamage <= 0
                && (advancedBreakdown == null || advancedBreakdown.requestedDamage() <= 0)) {
            return;
        }

        Entity actor = null;
        PDamageType damageType = null;
        if (event instanceof EntityDamageByEntityEvent byEntity) {
            actor = resolveActor(byEntity.getDamager());
            damageType = byEntity.getDamageType();
        }

        CombatSourceContext.Source context = CombatSourceContext.current();
        if (context != null && context.actor() != null) {
            actor = context.actor();
        }

        double appliedDamage = Math.min(rawDamage, Math.max(0, target.getHealth()));
        CombatSource advancedSource = event instanceof EntityDamageByEntityEvent byEntity
                ? byEntity.getCombatSource()
                : null;
        String sourceId = advancedSource != null
                ? advancedSource.attributionId()
                : context == null ? fallbackSourceId(event) : context.id();
        String sourceName = advancedSource != null
                ? advancedSource.displayName()
                : context == null ? fallbackSourceName(event) : context.displayName();
        String sourceType = advancedSource != null
                ? advancedSource.type().name()
                : context == null ? "FALLBACK" : context.kind().name();
        CombatSource causingSpell = advancedSource == null ? null : advancedSource.causingSpell();
        String causingSpellName = causingSpell == null || causingSpell == advancedSource
                ? null
                : causingSpell.displayName();
        CombatEvent combatEvent = new CombatEvent(
                EventKind.DAMAGE,
                actor,
                target,
                sourceId,
                sourceName,
                sourceType,
                causingSpellName,
                damageType == null ? fallbackType(event) : damageType.name(),
                rawDamage,
                appliedDamage,
                Math.max(0, target.getHealth() - appliedDamage),
                maxHealth(target),
                advancedBreakdown
        );
        distribute(combatEvent);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHeal(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof LivingEntity target)) {
            return;
        }
        double rawHealing = Math.max(0, event.getAmount());
        if (rawHealing <= 0) {
            return;
        }

        CombatSourceContext.Source context = CombatSourceContext.current();
        Entity actor = context == null || context.actor() == null ? target : context.actor();
        double maxHealth = maxHealth(target);
        double appliedHealing = Math.min(rawHealing, Math.max(0, maxHealth - target.getHealth()));
        if (appliedHealing <= 0) {
            return;
        }

        String reason = humanize(event.getRegainReason().name());
        CombatEvent combatEvent = new CombatEvent(
                EventKind.HEAL,
                actor,
                target,
                context == null ? "heal:" + event.getRegainReason().name() : context.id(),
                context == null ? reason : context.displayName(),
                context == null ? "HEAL" : context.kind().name(),
                null,
                "HEALING",
                rawHealing,
                appliedHealing,
                Math.min(maxHealth, target.getHealth() + appliedHealing),
                maxHealth,
                null
        );
        distribute(combatEvent);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        pending.remove(event.getPlayer().getUniqueId());
    }

    public Mode getMode(Player player) {
        String stored = player.getPersistentDataContainer().get(modeKey, PersistentDataType.STRING);
        return Mode.from(stored);
    }

    public void setMode(Player player, Mode mode) {
        if (mode == Mode.ALL && !player.hasPermission(GLOBAL_PERMISSION)) {
            mode = Mode.SELF;
        }
        pending.remove(player.getUniqueId());
        player.getPersistentDataContainer().set(modeKey, PersistentDataType.STRING, mode.name());
    }

    public boolean canViewAll(Player player) {
        return player.hasPermission(GLOBAL_PERMISSION);
    }

    public void flush(Player player) {
        LinkedHashMap<BatchKey, Batch> batches = pending.remove(player.getUniqueId());
        if (batches == null) {
            return;
        }
        batches.values().forEach(batch -> player.sendMessage(format(batch)));
    }

    public void shutdown() {
        Bukkit.getScheduler().cancelTask(flushTask);
        pending.clear();
    }

    private void distribute(CombatEvent event) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            Mode mode = getMode(viewer);
            if (mode == Mode.OFF) {
                continue;
            }
            boolean involved = is(viewer, event.actor()) || is(viewer, event.target());
            if (mode == Mode.SELF && !involved) {
                continue;
            }
            add(viewer, event, mode == Mode.ALL ? Perspective.GLOBAL : perspective(viewer, event));
        }
    }

    private void add(Player viewer, CombatEvent event, Perspective perspective) {
        String actorId = event.actor() == null ? "environment" : event.actor().getUniqueId().toString();
        BatchKey key = new BatchKey(event.kind(), perspective, actorId, event.target().getUniqueId(),
                event.sourceId(), event.type());
        LinkedHashMap<BatchKey, Batch> batches =
                pending.computeIfAbsent(viewer.getUniqueId(), ignored -> new LinkedHashMap<>());
        Batch batch = batches.get(key);
        long now = System.currentTimeMillis();
        if (batch != null && now - batch.startedAt >= MAX_BATCH_MILLIS) {
            viewer.sendMessage(format(batch));
            batches.remove(key);
            batch = null;
        }
        if (batch == null) {
            batch = new Batch(key, displayName(event.actor(), "Environment"), displayName(event.target(), "Target"),
                    event.sourceName(), event.sourceType(), event.causingSpellName(), now);
            batches.put(key, batch);
        }
        batch.add(event, now);
    }

    private void flushExpired() {
        long now = System.currentTimeMillis();
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            LinkedHashMap<BatchKey, Batch> batches = pending.get(viewer.getUniqueId());
            if (batches == null) {
                continue;
            }
            batches.entrySet().removeIf(entry -> {
                Batch batch = entry.getValue();
                if (now - batch.lastAt < QUIET_FLUSH_MILLIS && now - batch.startedAt < MAX_BATCH_MILLIS) {
                    return false;
                }
                viewer.sendMessage(format(batch));
                return true;
            });
            if (batches.isEmpty()) {
                pending.remove(viewer.getUniqueId());
            }
        }
    }

    private Component format(Batch batch) {
        boolean healing = batch.key.kind == EventKind.HEAL;
        TextColor amountColor = healing ? NamedTextColor.GREEN : damageColor(batch.key.type);
        NamedTextColor directionColor = switch (batch.key.perspective) {
            case OUTGOING -> NamedTextColor.GREEN;
            case INCOMING -> NamedTextColor.RED;
            case GLOBAL -> NamedTextColor.GOLD;
        };
        String actor = batch.key.perspective == Perspective.OUTGOING ? "You" : batch.actorName;
        String target = batch.key.perspective == Perspective.INCOMING ? "you" : batch.targetName;
        String count = batch.count > 1 ? batch.count + "× " : "";
        double seconds = (batch.lastAt - batch.startedAt) / 1_000.0;

        Component line = Component.text(healing ? "✦ " : "⚔ ", directionColor)
                .append(Component.text(actor + " → " + target + ": ", NamedTextColor.GRAY))
                .append(Component.text(count + batch.sourceName, NamedTextColor.WHITE))
                .append(Component.text(" · ", NamedTextColor.DARK_GRAY))
                .append(Component.text(formatNumber(batch.appliedTotal), amountColor))
                .append(Component.text(healing ? " healing" : " " + humanize(batch.key.type).toLowerCase(Locale.ROOT),
                        NamedTextColor.GRAY));
        if (batch.count > 1) {
            line = line.append(Component.text(" · " + formatNumber(seconds) + "s", NamedTextColor.DARK_GRAY));
        }
        return line.hoverEvent(HoverEvent.showText(formatHover(batch)));
    }

    private Component formatHover(Batch batch) {
        double average = batch.appliedTotal / batch.count;
        double overkill = Math.max(0, batch.rawTotal - batch.appliedTotal);
        StringBuilder details = new StringBuilder()
                .append(batch.sourceName).append('\n')
                .append("Source: ").append(humanize(batch.sourceType));
        if (batch.causingSpellName != null) {
            details.append('\n').append("Caused by spell: ").append(batch.causingSpellName);
        }
        details.append('\n').append("Events: ").append(batch.count).append('\n')
                .append("Applied: ").append(formatNumber(batch.appliedTotal)).append('\n')
                .append("Average: ").append(formatNumber(average)).append('\n')
                .append("Range: ").append(formatNumber(batch.minimum)).append(" – ")
                .append(formatNumber(batch.maximum)).append('\n')
                .append("Raw total: ").append(formatNumber(batch.rawTotal));
        if (overkill > 0.005) {
            details.append('\n').append("Overheal/overkill: ").append(formatNumber(overkill));
        }
        details.append('\n').append("Type: ").append(humanize(batch.key.type))
                .append('\n').append("Target health: ").append(formatNumber(batch.targetHealth))
                .append(" / ").append(formatNumber(batch.targetMaxHealth));
        if (batch.advancedEvents > 0) {
            details.append("\n\nPapyrus calculation")
                    .append('\n').append("Criticals: ").append(batch.criticalEvents)
                    .append(" / ").append(batch.advancedEvents)
                    .append('\n').append("Requested: ").append(formatNumber(batch.requestedTotal))
                    .append('\n').append("After critical: ").append(formatNumber(batch.afterCriticalTotal))
                    .append('\n').append("Avg resistance: ")
                    .append(formatNumber(batch.resistanceTotal / batch.advancedEvents))
                    .append('\n').append("Avg penetration: ")
                    .append(formatNumber(batch.penetrationTotal / batch.advancedEvents))
                    .append('\n').append("Avg effective resistance: ")
                    .append(formatNumber(batch.effectiveResistanceTotal / batch.advancedEvents))
                    .append('\n').append("After resistance: ")
                    .append(formatNumber(batch.afterResistanceTotal))
                    .append('\n').append("After Spellbook modifiers: ")
                    .append(formatNumber(batch.afterModifiersTotal))
                    .append('\n').append("Event final: ").append(formatNumber(batch.finalDamageTotal));
        }
        return Component.text(details.toString(), NamedTextColor.GRAY);
    }

    private static Entity resolveActor(Entity direct) {
        if (direct instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Entity entity) {
                return entity;
            }
        }
        return direct;
    }

    private static Perspective perspective(Player viewer, CombatEvent event) {
        return is(viewer, event.actor()) ? Perspective.OUTGOING : Perspective.INCOMING;
    }

    private static boolean is(Entity first, Entity second) {
        return first != null && second != null && first.getUniqueId().equals(second.getUniqueId());
    }

    private static String fallbackSourceId(EntityDamageEvent event) {
        if (event instanceof EntityDamageByEntityEvent byEntity && byEntity.getDamager() instanceof Projectile projectile) {
            return "projectile:" + projectile.getType().key();
        }
        return "damage:" + event.getCause().name();
    }

    private static String fallbackSourceName(EntityDamageEvent event) {
        if (event instanceof EntityDamageByEntityEvent byEntity && byEntity.getDamager() instanceof Projectile projectile) {
            return humanize(projectile.getType().name());
        }
        if (event instanceof EntityDamageByEntityEvent) {
            return "Attack";
        }
        return humanize(event.getCause().name());
    }

    private static String fallbackType(EntityDamageEvent event) {
        return switch (event.getCause()) {
            case FIRE, FIRE_TICK, LAVA, HOT_FLOOR -> "FIRE";
            default -> "PURE";
        };
    }

    private static TextColor damageColor(String type) {
        try {
            return DamageColor.getColorForDamageType(PDamageType.valueOf(type));
        } catch (IllegalArgumentException exception) {
            return NamedTextColor.RED;
        }
    }

    private static double maxHealth(LivingEntity entity) {
        AttributeInstance maxHealth = entity.getAttribute(Attribute.MAX_HEALTH);
        return maxHealth == null ? entity.getHealth() : maxHealth.getValue();
    }

    private static String displayName(Entity entity, String fallback) {
        return entity == null ? fallback : entity.getName();
    }

    private static String formatNumber(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.005) {
            return Long.toString(Math.round(value));
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static String humanize(String value) {
        String[] words = value.toLowerCase(Locale.ROOT).replace('-', '_').split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (!result.isEmpty()) {
                result.append(' ');
            }
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return result.isEmpty() ? "Unknown" : result.toString();
    }

    public enum Mode {
        OFF,
        SELF,
        ALL;

        private static Mode from(String value) {
            if (value == null) {
                return OFF;
            }
            try {
                return valueOf(value.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                return OFF;
            }
        }
    }

    private enum EventKind {
        DAMAGE,
        HEAL
    }

    private enum Perspective {
        OUTGOING,
        INCOMING,
        GLOBAL
    }

    private record CombatEvent(EventKind kind, Entity actor, LivingEntity target, String sourceId,
                               String sourceName, String sourceType, String causingSpellName,
                               String type, double rawAmount,
                               double appliedAmount, double targetHealth, double targetMaxHealth,
                               CombatDamageBreakdown breakdown) {
    }

    private record BatchKey(EventKind kind, Perspective perspective, String actorId, UUID targetId,
                            String sourceId, String type) {
    }

    private static final class Batch {
        private final BatchKey key;
        private final String actorName;
        private final String targetName;
        private final String sourceName;
        private final String sourceType;
        private final String causingSpellName;
        private final long startedAt;
        private long lastAt;
        private int count;
        private double rawTotal;
        private double appliedTotal;
        private double minimum = Double.MAX_VALUE;
        private double maximum;
        private double targetHealth;
        private double targetMaxHealth;
        private int advancedEvents;
        private int criticalEvents;
        private double requestedTotal;
        private double afterCriticalTotal;
        private double resistanceTotal;
        private double penetrationTotal;
        private double effectiveResistanceTotal;
        private double afterResistanceTotal;
        private double afterModifiersTotal;
        private double finalDamageTotal;

        private Batch(BatchKey key, String actorName, String targetName, String sourceName, String sourceType,
                      String causingSpellName, long startedAt) {
            this.key = key;
            this.actorName = actorName;
            this.targetName = targetName;
            this.sourceName = sourceName;
            this.sourceType = sourceType;
            this.causingSpellName = causingSpellName;
            this.startedAt = startedAt;
        }

        private void add(CombatEvent event, long now) {
            count++;
            rawTotal += event.rawAmount();
            appliedTotal += event.appliedAmount();
            minimum = Math.min(minimum, event.appliedAmount());
            maximum = Math.max(maximum, event.appliedAmount());
            targetHealth = event.targetHealth();
            targetMaxHealth = event.targetMaxHealth();
            CombatDamageBreakdown breakdown = event.breakdown();
            if (breakdown != null) {
                advancedEvents++;
                if (breakdown.critical()) {
                    criticalEvents++;
                }
                requestedTotal += breakdown.requestedDamage();
                afterCriticalTotal += breakdown.damageAfterCritical();
                resistanceTotal += breakdown.resistance();
                penetrationTotal += breakdown.penetration();
                effectiveResistanceTotal += breakdown.effectiveResistance();
                afterResistanceTotal += breakdown.damageAfterResistance();
                afterModifiersTotal += breakdown.damageAfterModifiers();
                finalDamageTotal += breakdown.finalDamage();
            }
            lastAt = now;
        }
    }
}
