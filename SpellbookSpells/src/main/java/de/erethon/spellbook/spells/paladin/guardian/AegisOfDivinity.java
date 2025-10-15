package de.erethon.spellbook.spells.paladin.guardian;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.aoe.AoE;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.paladin.PaladinBaseSpell;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.util.Vector;

import java.awt.*;
import java.util.HashSet;
import java.util.Set;

public class AegisOfDivinity extends GuardianBaseSpell implements Listener {

    // The Guardian creates a massive sanctuary of divine protection, forming concentric rings of power.
    // The inner sanctum grants resurrection and massive healing, while outer rings push enemies away and destroy projectiles.
    // Pillars of light erupt from the ground, creating a truly divine spectacle befitting an ultimate ability.

    private final double innerRadius = data.getDouble("innerRadius", 8);
    private final double outerRadius = data.getDouble("outerRadius", 16);
    private final double resurrectionRange = data.getDouble("resurrectionRange", 20);
    private final double healMin = data.getDouble("healMin", 30);
    private final double healMax = data.getDouble("healMax", 60);
    private final double pushForce = data.getDouble("pushForce", 2.0);
    private final int sanctuaryDuration = data.getInt("sanctuaryDuration", 300);
    private final int pillarHeight = data.getInt("pillarHeight", 8);

    private Location center;
    private AoE innerSanctum;
    private AoE outerBarrier;
    private AoE projectileDestroyer;
    private int effectTick = 20;
    private int pillarTick = 40;
    private final Set<Player> alreadyResurrected = new HashSet<>();

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
        Bukkit.getPluginManager().registerEvents(this, Spellbook.getInstance().getImplementer());
        center = caster.getLocation();

        caster.getWorld().playSound(center, Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.RECORDS, 2.0f, 0.3f);
        caster.getWorld().playSound(center, Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.RECORDS, 1.5f, 0.5f);
        caster.getWorld().playSound(center, Sound.ENTITY_WITHER_SPAWN, SoundCategory.RECORDS, 0.8f, 2.0f);

        innerSanctum = createCircularAoE(center, innerRadius, 2, sanctuaryDuration)
                .onTick(aoe -> {
                    if (caster.getTicksLived() % 10 == 0) {
                        aoe.getCenter().getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING,
                            aoe.getCenter().add(0, 3, 0),
                            20, innerRadius * 0.6, 2, innerRadius * 0.6, 0.1);
                    }
                    if (caster.getTicksLived() % 8 == 0) {
                        aoe.getCenter().getWorld().spawnParticle(Particle.END_ROD,
                            aoe.getCenter().add(0, 1.5, 0),
                            15, innerRadius * 0.5, 1.5, innerRadius * 0.5, 0.05);
                    }
                })
                .addBlockChange(Material.GOLD_BLOCK, Material.BEACON, Material.DIAMOND_BLOCK)
                .sendBlockChanges();

        outerBarrier = createCircularAoE(center, outerRadius, 1, sanctuaryDuration)
                .onTick(aoe -> {
                    if (caster.getTicksLived() % 15 == 0) {
                        aoe.getCenter().getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME,
                            aoe.getCenter().add(0, 4, 0),
                            25, outerRadius * 0.7, 3, outerRadius * 0.7, 0.08);
                    }
                })
                .onEnter((aoe, entity) -> {
                    if (entity instanceof LivingEntity target && Spellbook.canAttack(caster, target)) {
                        Vector pushDirection = target.getLocation().toVector().subtract(center.toVector()).normalize();
                        pushDirection.setY(0.3); // Add slight upward force
                        target.setVelocity(pushDirection.multiply(pushForce));

                        target.getWorld().spawnParticle(Particle.FLASH,
                            target.getLocation(),
                            1, 0, 0, 0, 0, Color.ORANGE);
                        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.RECORDS, 0.5f, 2.0f);
                    }
                })
                .addBlockChange(Material.WHITE_STAINED_GLASS, Material.YELLOW_STAINED_GLASS, Material.ORANGE_STAINED_GLASS)
                .sendBlockChanges();

        projectileDestroyer = createCircularAoE(center, outerRadius + 2, 3, sanctuaryDuration)
                .onTick(aoe -> destroyHostileProjectiles());

        center.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, center, 3, 2, 1, 2, 0);
        center.getWorld().spawnParticle(Particle.FLASH, center, 1, 0, 0, 0, 0, Color.ORANGE);

        return super.onCast();
    }

    @Override
    protected void onTick() {
        effectTick--;
        pillarTick--;

        if (effectTick <= 0) {
            effectTick = 20;
            healAlliesInSanctum();

            center.getWorld().playSound(center, Sound.BLOCK_BEACON_AMBIENT, SoundCategory.RECORDS, 0.8f, 1.5f);
        }

        if (pillarTick <= 0) {
            pillarTick = 40;
            createLightPillars();
        }

        super.onTick();
    }

    private void healAlliesInSanctum() {
        for (LivingEntity target : center.getNearbyLivingEntities(innerRadius)) {
            if (target.equals(caster)) continue;
            if (Spellbook.canAttack(caster, target)) continue;

            double heal = Spellbook.getRangedValue(data, caster, target, Attribute.STAT_HEALINGPOWER, healMin, healMax, "heal");
            heal += caster.getEnergy() * 0.5;

            double currentHealth = target.getHealth();
            double maxHealth = target.getAttribute(Attribute.MAX_HEALTH).getValue();
            target.setHealth(Math.min(maxHealth, currentHealth + heal));

            target.getWorld().spawnParticle(Particle.HEART,
                target.getLocation().add(0, 2.5, 0),
                4, 0.5, 0.5, 0.5, 0);
        }
    }

    private void createLightPillars() {
        for (int i = 0; i < 6; i++) {
            double angle = Math.PI * 2 * i / 6;
            double pillarRadius = innerRadius * 0.7;
            double x = center.getX() + pillarRadius * Math.cos(angle);
            double z = center.getZ() + pillarRadius * Math.sin(angle);

            Location pillarBase = new Location(center.getWorld(), x, center.getY(), z);

            for (int y = 0; y < pillarHeight; y++) {
                Location pillarLoc = pillarBase.clone().add(0, y, 0);
                pillarLoc.getWorld().spawnParticle(Particle.END_ROD,
                    pillarLoc,
                    3, 0.2, 0.2, 0.2, 0.02);
            }

            pillarBase.getWorld().playSound(pillarBase, Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.RECORDS, 0.4f, 0.8f);
        }
    }

    private void destroyHostileProjectiles() {
        for (Projectile projectile : center.getNearbyEntitiesByType(Projectile.class, outerRadius + 2)) {
            if (projectile.getShooter() instanceof LivingEntity shooter && Spellbook.canAttack(caster, shooter)) {
                projectile.getWorld().spawnParticle(Particle.FLASH, projectile.getLocation(), 1, 0, 0, 0, 0, Color.YELLOW);
                projectile.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, projectile.getLocation(), 8, 0.5, 0.5, 0.5, 0.1);
                projectile.getWorld().playSound(projectile.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.RECORDS, 0.3f, 2.0f);
                projectile.remove();
            }
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

        if (deadPlayer.getLocation().distance(center) > innerRadius) {
            deadPlayer.teleport(center);
        }

        deadPlayer.setHealth(deadPlayer.getAttribute(Attribute.MAX_HEALTH).getValue());

        deadPlayer.getWorld().playSound(deadPlayer.getLocation(), Sound.ITEM_TOTEM_USE, SoundCategory.RECORDS, 1.5f, 0.8f);
        deadPlayer.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, deadPlayer.getLocation(), 50, 1, 2, 1, 0.2);
        deadPlayer.getWorld().spawnParticle(Particle.FLASH, deadPlayer.getLocation(), 2, 0.5, 1, 0.5, 0, Color.ORANGE);

        for (int y = 0; y < 10; y++) {
            Location pillarLoc = deadPlayer.getLocation().clone().add(0, y, 0);
            pillarLoc.getWorld().spawnParticle(Particle.END_ROD, pillarLoc, 5, 0.3, 0.3, 0.3, 0.05);
        }

        alreadyResurrected.add(deadPlayer);
    }

    @Override
    protected void cleanup() {
        if (innerSanctum != null) {
            innerSanctum.revertBlockChanges();
        }
        if (outerBarrier != null) {
            outerBarrier.revertBlockChanges();
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
