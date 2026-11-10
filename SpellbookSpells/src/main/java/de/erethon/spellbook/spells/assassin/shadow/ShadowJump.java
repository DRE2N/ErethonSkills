package de.erethon.spellbook.spells.assassin.shadow;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.vfx.SkillSignatureVfx;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Input;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;

/** Readable directional dash. Crossing a foe earns a brief defensive buffer. */
public class ShadowJump extends ShadowBaseSpell implements Listener {

    private final int dashTicks = data.getInt("durationTicks", 15);
    private final double dashSpeed = data.getDouble("dashMultiplier", 1.1);
    private final double sideStrength = data.getDouble("sideDashStrength", 0.35);
    private final int bufferTicks = data.getInt("bufferTicks", 20);
    private final double baseBufferReduction = data.getDouble("baseBufferReduction", 0.1);
    private final double resistanceCoefficient = data.getDouble("resistanceCoefficient", 0.0015);
    private Location start;
    private int bufferUntil;

    public ShadowJump(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = dashTicks + bufferTicks;
        tickInterval = 1;
    }

    @Override
    public boolean onCast() {
        if (!super.onCast()) return false;
        start = caster.getLocation().clone();
        double yaw = Math.toRadians(start.getYaw());
        Vector forward = new Vector(-Math.sin(yaw), 0, Math.cos(yaw));
        Vector side = new Vector(-forward.getZ(), 0, forward.getX());
        Vector dash = forward.multiply(dashSpeed);
        if (caster instanceof Player player) {
            Input input = player.getCurrentInput();
            if (input.isLeft()) dash.subtract(side.clone().multiply(sideStrength));
            if (input.isRight()) dash.add(side.clone().multiply(sideStrength));
        }
        if (dash.lengthSquared() > 1.5) dash.normalize().multiply(1.2);
        dash.setY(caster.getVelocity().getY());
        caster.setVelocity(dash);
        caster.getWorld().playSound(start, Sound.ENTITY_PHANTOM_FLAP, 0.8f, 1.4f);
        SkillSignatureVfx.accent(start.clone().add(0, 0.9, 0), getClass(),
                "wake", 2.2f, 15, false);
        Bukkit.getPluginManager().registerEvents(this, Spellbook.getInstance().getImplementer());
        triggerTraits(0);
        return true;
    }

    @Override
    protected void onTick() {
        super.onTick();
        if (currentTicks > dashTicks) return;
        if (currentTicks % 3 == 0) SkillSignatureVfx.accent(
                caster.getLocation().add(0, 0.7, 0), getClass(), "wake", 1.3f, 10, false);
        if (bufferUntil > 0 || caster.getLocation().distanceSquared(start) < 2.25) return;
        for (LivingEntity entity : caster.getLocation().getNearbyLivingEntities(1.5)) {
            if (entity != caster && Spellbook.canAttack(caster, entity)
                    && caster.getBoundingBox().overlaps(entity.getBoundingBox())) {
                bufferUntil = currentTicks + bufferTicks;
                caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME,
                        0.65f, 1.25f);
                SkillSignatureVfx.accent(caster.getLocation().add(0, 1, 0), getClass(),
                        "main", 2.0f, 14, false);
                triggerTraits(1);
                break;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() != caster || currentTicks >= bufferUntil || bufferUntil <= 0) return;
        double physical = caster.getAttribute(Attribute.RESISTANCE_PHYSICAL) == null ? 0
                : caster.getAttribute(Attribute.RESISTANCE_PHYSICAL).getValue();
        double magical = caster.getAttribute(Attribute.RESISTANCE_MAGICAL) == null ? 0
                : caster.getAttribute(Attribute.RESISTANCE_MAGICAL).getValue();
        double reduction = Math.min(0.25, baseBufferReduction
                + Math.max(0, Math.max(physical, magical)) * resistanceCoefficient);
        event.setDamage(event.getDamage() * (1.0 - reduction));
    }

    @Override
    protected void cleanup() {
        HandlerList.unregisterAll(this);
        super.cleanup();
    }
}
