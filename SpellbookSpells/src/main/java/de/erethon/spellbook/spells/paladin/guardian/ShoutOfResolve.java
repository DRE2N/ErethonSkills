package de.erethon.spellbook.spells.paladin.guardian;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** A short team call that blunts one committed hit for each nearby ally. */
public class ShoutOfResolve extends GuardianBaseSpell implements Listener {

    private final double range = data.getDouble("range", 6);
    private final int activeTicks = data.getInt("activeTicks", 40);
    private final double basePrevented = data.getDouble("basePrevented", 10);
    private final double resistanceCoefficient = data.getDouble("resistanceCoefficient", 0.2);
    private final Set<UUID> protectedTargets = new HashSet<>();

    public ShoutOfResolve(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = activeTicks;
        tickInterval = 1;
    }

    @Override
    public boolean onCast() {
        if (!super.onCast()) return false;
        Bukkit.getPluginManager().registerEvents(this, Spellbook.getInstance().getImplementer());
        caster.getWorld().playSound(caster.getLocation(), Sound.ITEM_GOAT_HORN_SOUND_0, 0.9f, 1.1f);
        caster.getWorld().spawnParticle(Particle.END_ROD, caster.getLocation().add(0, 1, 0),
                8, 1.2, 0.8, 1.2, 0.04);
        return true;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onIncomingHit(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity target) || event.getDamage() <= 0
                || target.getWorld() != caster.getWorld()
                || target.getLocation().distanceSquared(caster.getLocation()) > range * range
                || Spellbook.canAttack(caster, target)
                || !protectedTargets.add(target.getUniqueId())) return;
        double resistance = Math.max(0, caster.getAttribute(Attribute.RESISTANCE_PHYSICAL).getValue())
                + Math.max(0, caster.getAttribute(Attribute.RESISTANCE_MAGICAL).getValue());
        double prevented = Math.min(event.getDamage() * 0.5,
                Math.min(target.getMaxHealth() * 0.2,
                        basePrevented + Math.min(100, resistance) * resistanceCoefficient));
        event.setDamage(Math.max(0, event.getDamage() - prevented));
        target.getWorld().playSound(target.getLocation(), Sound.ITEM_SHIELD_BLOCK, 0.9f, 1.2f);
        target.getWorld().spawnParticle(Particle.ENCHANTED_HIT, target.getEyeLocation(),
                4, 0.3, 0.4, 0.3, 0.05);
    }

    @Override
    protected void cleanup() {
        HandlerList.unregisterAll(this);
        super.cleanup();
    }
}
