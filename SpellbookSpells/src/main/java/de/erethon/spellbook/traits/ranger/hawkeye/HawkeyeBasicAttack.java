package de.erethon.spellbook.traits.ranger.hawkeye;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.util.BoundingBox;

public class HawkeyeBasicAttack extends SpellTrait implements Listener {
    
    // The Hawkeye deals bonus damage based on the distance to the target.
    // Additionally, hitting the head of the target deals 50% more damage and adds flow state.

    private final double bonusDamagePerBlock = data.getDouble("bonusDamagePerBlock", 0.1);
    private final double headshotRangeFromEye = data.getDouble("headshotRangeFromEye", 0.33);
    private final double headshotMultiplier = data.getDouble("headshotMultiplier", 1.5);

    private int flowTick = 0;

    public HawkeyeBasicAttack(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @EventHandler
    private void onProjectileHit(ProjectileHitEvent  event) {
        if (event.getHitEntity() == null || !(event.getHitEntity() instanceof LivingEntity living)) {
            return;
        }
        if (living == caster) {
            return;
        }
        if (event.getEntity().getShooter() == null || !(event.getEntity().getShooter() instanceof LivingEntity shooter) || shooter != caster) {
            return;
        }
        double distance = caster.getLocation().distance(living.getLocation());
        double damage = distance * bonusDamagePerBlock;
        // Check for headshot
        Location location = living.getEyeLocation();
        BoundingBox boundingBox = new BoundingBox(location.getX() - headshotRangeFromEye, location.getY() - headshotRangeFromEye, location.getZ() - headshotRangeFromEye,
                location.getX() + headshotRangeFromEye, location.getY() + headshotRangeFromEye, location.getZ() + headshotRangeFromEye);

        if (boundingBox.contains(event.getEntity().getLocation().toVector())) {
            damage *= headshotMultiplier;
            caster.getWorld().spawnParticle(Particle.CRIT, event.getHitEntity().getLocation(), 3, 0.5, 0.5, 0.5);
            caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_ANVIL_HIT, 1, 1);
            caster.getWorld().playSound(event.getHitEntity(), Sound.ENTITY_ARROW_HIT_PLAYER, 1, 1);
            caster.getTags().add("spellbook.ranger.flow");
        }

        living.damage(damage, caster, PDamageType.MAGIC);
    }

    // Handle flow visuals here, don't need a new trait for that
    @Override
    protected void onTick() {
        flowTick++;
        if (flowTick >= 20) {
            flowTick = 0;
            if (caster.getTags().contains("spellbook.ranger.flow")) {
                caster.getLocation().getWorld().spawnParticle(Particle.TINTED_LEAVES, caster.getLocation(), 5, 0.5, 0.5, 0.5);
            }
        }
    }

    @Override
    protected void onAdd() {
        Bukkit.getPluginManager().registerEvents(this, Spellbook.getInstance().getImplementer());
    }

    @Override
    protected void onRemove() {
        HandlerList.unregisterAll(this);
    }
}
