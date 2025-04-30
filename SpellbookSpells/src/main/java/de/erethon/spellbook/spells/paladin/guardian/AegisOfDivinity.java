package de.erethon.spellbook.spells.paladin.guardian;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.paladin.PaladinBaseSpell;
import de.slikey.effectlib.effect.LineEffect;
import de.slikey.effectlib.effect.SphereEffect;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.HashSet;
import java.util.Set;

public class AegisOfDivinity extends PaladinBaseSpell implements Listener {

    // The Guardian creates a large dome of protection at a location, pushing enemies away and strongly healing allies for its duration.
    // The dome also destroys any enemy projectiles that enter it.
    // Players that are close to the dome or inside it cannot die and are teleported to the center of the dome when they are about to die.
    // Players can only be resurrected once.

    private final double radius = data.getDouble("radius", 16);
    private final double resurrectionRange = data.getDouble("resurrectionRange", 32);
    private final double healMin = data.getDouble("healMin", 25);
    private final double healMax = data.getDouble("healMax", 40);

    private Location center;
    private int effectTick = 20;
    private final Set<Player> alreadyResurrected = new HashSet<>();

    public AegisOfDivinity(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = duration * 20;
    }

    @Override
    protected boolean onPrecast() {
        return super.onPrecast() && hasEnergy(caster, data); // 100 or 90
    }

    @Override
    public boolean onCast() {
        Bukkit.getPluginManager().registerEvents(this, Spellbook.getInstance().getImplementer());
        center = caster.getLocation();
        SphereEffect sphereEffect = new SphereEffect(Spellbook.getInstance().getEffectManager());
        sphereEffect.setLocation(center);
        sphereEffect.radius = radius;
        sphereEffect.yOffset = radius / 2; // Center the sphere below the caster
        sphereEffect.particle = Particle.DUST;
        sphereEffect.particleCount = 128;
        sphereEffect.color = Color.YELLOW;
        sphereEffect.duration = duration * 20;
        sphereEffect.start();
        return super.onCast();
    }

    @Override
    protected void onTick() {
        destroyProjectiles();
        pushEnemiesOut();
        drawParticleLines();
        if (effectTick > 0) {
            effectTick--;
            return;
        }
        effectTick = 20;

        for (LivingEntity target : center.getNearbyLivingEntities(radius)) {
            if (target.equals(caster)) continue;
            if (Spellbook.canAttack(caster, target)) continue;
            double heal = Spellbook.getRangedValue(data, caster, target, Attribute.STAT_HEALINGPOWER, healMin, healMax, "heal");
            target.heal(heal);
        }
    }

    @EventHandler
    private void onDeath(PlayerDeathEvent event) {
        Player deadPlayer = event.getEntity();
        if (alreadyResurrected.contains(deadPlayer)) return;
        if (deadPlayer.equals(caster)) return;
        if (deadPlayer.getWorld() != center.getWorld()) return;
        if (deadPlayer.getLocation().distance(center) > resurrectionRange) return;
        if (Spellbook.canAttack(caster, deadPlayer)) return;
        event.setCancelled(true);
        if (deadPlayer.getLocation().distance(center) > radius) {
            deadPlayer.teleport(center);
        }
        deadPlayer.setHealth(deadPlayer.getAttribute(Attribute.MAX_HEALTH).getValue());
        deadPlayer.getWorld().playSound(deadPlayer.getLocation(), Sound.ITEM_TOTEM_USE, 1, 1);
        deadPlayer.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, deadPlayer.getLocation(), 2, 0.5, 0.5, 0.5, 0.1);
        alreadyResurrected.add(deadPlayer);
    }

    private void drawParticleLines() {
        for (Player player : center.getNearbyPlayers(resurrectionRange)) {
            if (alreadyResurrected.contains(player)) continue;
            if (Spellbook.canAttack(caster, player)) continue;
            // Draw a line from the player to the center of the dome
            double distance = center.distance(player.getLocation());
            LineEffect lineEffect = new LineEffect(Spellbook.getInstance().getEffectManager());
            lineEffect.setEntity(player);
            lineEffect.setTarget(center);
            lineEffect.particle = Particle.DUST;
            lineEffect.particleCount = (int) distance;
            lineEffect.color = Color.ORANGE;
            lineEffect.duration = 1;
            lineEffect.start();
        }
    }

    private void destroyProjectiles() {
        for (Projectile projectile : center.getNearbyEntitiesByType(Projectile.class, radius)) {
            if (projectile.getShooter() instanceof LivingEntity shooter && Spellbook.canAttack(caster, shooter)) {
                projectile.getWorld().spawnParticle(Particle.WHITE_SMOKE, projectile.getLocation(), 4, 1, 1, 1);
                projectile.getWorld().playSound(projectile.getLocation(), Sound.ENTITY_ITEM_BREAK, 1, 1);
                projectile.remove();
            }
        }
    }

    private void pushEnemiesOut() {
        for (Entity entity : center.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof LivingEntity living && Spellbook.canAttack(caster, living)) {
                double distance = center.distance(living.getLocation());
                if (distance < radius) {
                    double pushStrength = (radius - distance) / radius;
                    living.setVelocity(living.getLocation().toVector().subtract(center.toVector()).normalize().multiply(pushStrength));
                }
            }
        }
    }

    @Override
    protected void cleanup() {
        super.cleanup();
        HandlerList.unregisterAll(this);
    }
}
