package de.erethon.spellbook.spells.paladin.guardian;
import de.erethon.spellbook.utils.TargetedParticles;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.aoe.AoE;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.combat.BossControl;
import de.erethon.spellbook.combat.HealingContext;
import de.erethon.spellbook.spells.vfx.SkillVfx;
import de.erethon.spellbook.spells.vfx.TerrainRing;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Display;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.util.Vector;

import java.awt.*;
import java.util.List;

public class AegisOfDivinity extends GuardianBaseSpell implements Listener {

    // The Guardian creates a massive sanctuary of divine protection, forming concentric rings of power.
    // The inner sanctum grants resurrection and massive healing, while outer rings push enemies away and destroy projectiles.
    // Pillars of light erupt from the ground, creating a truly divine spectacle befitting an ultimate ability.

    private final double innerRadius = data.getDouble("innerRadius", 8);
    private final double outerRadius = data.getDouble("outerRadius", 16);
    private final double resurrectionRange = data.getDouble("resurrectionRange", 20);
    private final double healMin = data.getDouble("healMin", 30);
    private final double healMax = data.getDouble("healMax", 60);
    private final int sanctuaryDuration = data.getInt("sanctuaryDuration", 300);
    private final int pillarHeight = data.getInt("pillarHeight", 8);

    private Location center;
    private AoE innerSanctum;
    private AoE outerBarrier;
    private AoE projectileDestroyer;
    private boolean rescueUsed;
    private int projectileBudget = 6;
    private TerrainRing innerBoundary;
    private TerrainRing outerBoundary;

    public AegisOfDivinity(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = duration * 20;
    }

    @Override
    protected boolean onPrecast() {
        return super.onPrecast() && hasEnergy(caster, data);
    }

    @Override
    public boolean onCast() {
        if (!super.onCast()) return false;
        Bukkit.getPluginManager().registerEvents(this, Spellbook.getInstance().getImplementer());
        center = caster.getLocation();

        caster.getWorld().playSound(center, Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.RECORDS, 2.0f, 0.3f);
        caster.getWorld().playSound(center, Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.RECORDS, 1.5f, 0.5f);
        caster.getWorld().playSound(center, Sound.ENTITY_WITHER_SPAWN, SoundCategory.RECORDS, 0.8f, 2.0f);

        innerSanctum = createCircularAoE(center, innerRadius, 2, sanctuaryDuration)
                .onTick(aoe -> { });

        outerBarrier = createCircularAoE(center, outerRadius, 1, sanctuaryDuration)
                .onEnter((aoe, entity) -> {
                    if (entity instanceof LivingEntity target && Spellbook.canAttack(caster, target)) {
                        if (!BossControl.apply(caster, target, 20)) {
                            Vector pushDirection = target.getLocation().toVector().subtract(center.toVector()).normalize();
                            pushDirection.setY(0.15);
                            target.setVelocity(pushDirection.multiply(target instanceof Player ? 0.45 : 0.8));
                        }

                        target.getWorld().spawnParticle(Particle.FLASH,
                            target.getLocation(),
                            1, 0, 0, 0, 0, Color.CYAN);
                        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.RECORDS, 0.5f, 2.0f);
                    }
                });

        projectileDestroyer = createCircularAoE(center, outerRadius + 2, 3, sanctuaryDuration)
                .onTick(aoe -> destroyHostileProjectiles());

        if (SkillVfx.availableTo(caster)) {
            innerBoundary = new TerrainRing(center, innerRadius,
                    "terrain_band_guardian", 64, 0.7f);
            outerBoundary = new TerrainRing(center, outerRadius,
                    "terrain_band_guardian", 96, 0.8f);
        }
        healAlliesInSanctum();
        center.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, center.clone().add(0, 1, 0),
                10, 2, 1, 2, 0.08);
        center.getWorld().spawnParticle(Particle.FLASH, center, 1, 0, 0, 0, 0, Color.CYAN);

        return true;
    }

    private void healAlliesInSanctum() {
        for (LivingEntity target : center.getNearbyLivingEntities(innerRadius)) {
            if (Spellbook.canAttack(caster, target)) continue;

            double heal = Spellbook.getRangedValue(data, caster, target, Attribute.STAT_HEALINGPOWER, healMin, healMax, "heal");
            heal = Math.min(target.getMaxHealth() * 0.35, heal);
            if (HealingContext.isPlayerCombat(caster, target)) heal *= 0.65;

            double currentHealth = target.getHealth();
            double maxHealth = target.getAttribute(Attribute.MAX_HEALTH).getValue();
            target.setHealth(Math.min(maxHealth, currentHealth + heal));

            TargetedParticles.spawnForOthers(Particle.HEART, target,
                target.getLocation().add(0, 2.5, 0),
                4, 0.5, 0.5, 0.5);
        }
    }

    private void destroyHostileProjectiles() {
        if (projectileBudget <= 0) return;
        for (Projectile projectile : center.getNearbyEntitiesByType(Projectile.class, outerRadius + 2)) {
            if (projectile.getShooter() instanceof LivingEntity shooter && Spellbook.canAttack(caster, shooter)) {
                projectile.getWorld().spawnParticle(Particle.FLASH, projectile.getLocation(), 1, 0, 0, 0, 0, Color.CYAN);
                projectile.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, projectile.getLocation(),
                        4, 0.5, 0.5, 0.5, 0.1);
                projectile.getWorld().playSound(projectile.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.RECORDS, 0.3f, 2.0f);
                projectile.remove();
                if (--projectileBudget <= 0) break;
            }
        }
    }

    @EventHandler
    private void onDeath(PlayerDeathEvent event) {
        Player deadPlayer = event.getEntity();
        if (rescueUsed) return;
        if (deadPlayer.getWorld() != center.getWorld()) return;
        if (deadPlayer.getLocation().distance(center) > resurrectionRange) return;
        if (Spellbook.canAttack(caster, deadPlayer)) return;

        event.setCancelled(true);
        rescueUsed = true;

        if (deadPlayer.getLocation().distance(center) > innerRadius) {
            deadPlayer.teleport(center);
        }

        deadPlayer.setHealth(Math.max(1, deadPlayer.getAttribute(Attribute.MAX_HEALTH).getValue() * 0.2));

        deadPlayer.getWorld().playSound(deadPlayer.getLocation(), Sound.ITEM_TOTEM_USE, SoundCategory.RECORDS, 1.5f, 0.8f);
        deadPlayer.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, deadPlayer.getLocation(),
                20, 1, 2, 1, 0.2);
        deadPlayer.getWorld().spawnParticle(Particle.FLASH, deadPlayer.getLocation(), 2, 0.5, 1, 0.5, 0, Color.CYAN);

    }

    @Override
    protected void cleanup() {
        if (innerBoundary != null) innerBoundary.close();
        if (outerBoundary != null) outerBoundary.close();
        if (innerSanctum != null) {
            innerSanctum.end();
        }
        if (outerBarrier != null) {
            outerBarrier.end();
        }
        if (projectileDestroyer != null) {
            projectileDestroyer.end();
        }
        HandlerList.unregisterAll(this);
        super.cleanup();
    }

    @Override
    protected void addSpellPlaceholders() {
        spellAddedPlaceholders.add(Component.text(Spellbook.getRangedValue(data, caster, Attribute.STAT_HEALINGPOWER, healMin, healMax, "heal"), ATTR_HEALING_POWER_COLOR));
        placeholderNames.add("heal");
        spellAddedPlaceholders.add(Component.text(innerRadius, VALUE_COLOR));
        placeholderNames.add("innerRadius");
        spellAddedPlaceholders.add(Component.text(outerRadius, VALUE_COLOR));
        placeholderNames.add("outerRadius");
    }
}
