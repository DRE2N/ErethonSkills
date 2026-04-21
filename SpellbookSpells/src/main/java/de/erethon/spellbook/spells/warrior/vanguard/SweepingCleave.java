package de.erethon.spellbook.spells.warrior.vanguard;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class SweepingCleave extends VanguardBaseSpell {

    private static final String SEISMIC_TAG = "vanguard.seismic";
    private static final Color AFTERSHOCK_COLOR = Color.fromRGB(200, 50, 50);

    private final double coneRange = data.getDouble("coneRange", 6.0);
    private final double coneAngle = data.getDouble("coneAngle", 120.0);
    private final double coneHeight = data.getDouble("coneHeight", 3.0);
    private final double minKnockback = data.getDouble("minKnockback", 0.8);
    private final double maxKnockback = data.getDouble("maxKnockback", 1.5);
    private final double aftershockMultiplier = data.getDouble("aftershockMultiplier", 1.4);

    public SweepingCleave(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    public boolean onCast() {
        Vector direction = caster.getLocation().getDirection();

        ItemDisplay swingDisplay = spawnSwingDisplay();
        playWindupEffect();

        createConeAoE(caster.getLocation(), coneRange, coneAngle, coneHeight, direction, 20)
                .onEnter((aoe, entity) -> {
                    if (!entity.equals(caster) && Spellbook.canAttack(caster, entity)) {
                        double physicalDamage = Spellbook.getVariedAttributeBasedDamage(data, caster, entity, true, Attribute.ADVANTAGE_PHYSICAL);

                        if (entity.getTags().contains(SEISMIC_TAG)) {
                            entity.getTags().remove(SEISMIC_TAG);
                            physicalDamage *= aftershockMultiplier;
                            playAftershockEffect(entity);
                        }

                        entity.damage(physicalDamage, caster, PDamageType.PHYSICAL);
                        applyKnockback(entity, direction);
                        playHitEffect(entity);
                    }
                });

        animateSwingArc(swingDisplay, direction);
        playSwingEffect(direction);

        return super.onCast();
    }

    private ItemDisplay spawnSwingDisplay() {
        Location loc = caster.getLocation().add(0, 1.5, 0);
        return loc.getWorld().spawn(loc, ItemDisplay.class, d -> {
            d.setItemStack(new ItemStack(Material.IRON_SWORD));
            d.setBillboard(Display.Billboard.FIXED);
            d.setPersistent(false);
            d.setGlowing(true);
            d.setInterpolationDuration(2);
            d.setTeleportDuration(2);
            Vector dir = caster.getLocation().getDirection();
            float yaw = (float) Math.atan2(-dir.getX(), dir.getZ());
            d.setTransformation(new Transformation(
                new Vector3f(0, 0.5f, 0),
                new Quaternionf().rotateY(yaw).rotateX(-(float) Math.PI / 4),
                new Vector3f(1.5f, 1.5f, 1.5f),
                new Quaternionf()
            ));
        });
    }

    private void animateSwingArc(ItemDisplay display, Vector direction) {
        new BukkitRunnable() {
            int tick = 0;
            final float totalArc = (float) Math.toRadians(coneAngle);
            final float startYaw = (float) Math.atan2(-direction.getX(), direction.getZ()) - totalArc / 2;

            @Override
            public void run() {
                if (tick >= 10 || !display.isValid()) {
                    if (display.isValid()) display.remove();
                    cancel();
                    return;
                }
                float progress = tick / 10.0f;
                float currentYaw = startYaw + totalArc * progress;
                Location casterLoc = caster.getLocation().add(0, 1.5, 0);
                display.teleport(casterLoc);

                display.setInterpolationDelay(-1);
                display.setTransformation(new Transformation(
                    new Vector3f((float) Math.sin(currentYaw) * -1.2f, 0.3f, (float) Math.cos(currentYaw) * 1.2f),
                    new Quaternionf().rotateY(currentYaw).rotateX(-(float) Math.PI / 5),
                    new Vector3f(1.4f, 1.4f, 1.4f),
                    new Quaternionf()
                ));

                casterLoc.getWorld().spawnParticle(Particle.SWEEP_ATTACK,
                    casterLoc.clone().add(Math.sin(currentYaw) * -1.8, 0, Math.cos(currentYaw) * 1.8),
                    1, 0.1, 0.1, 0.1, 0);
                tick++;
            }
        }.runTaskTimer(Spellbook.getInstance().getImplementer(), 0L, 1L);
    }

    private void playWindupEffect() {
        caster.getWorld().spawnParticle(Particle.SWEEP_ATTACK, caster.getLocation().add(0, 1, 0), 3, 1.5, 0.5, 1.5, 0);
        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.8f, 0.9f);
    }

    private void playSwingEffect(Vector direction) {
        Vector perpendicular = new Vector(-direction.getZ(), 0, direction.getX()).normalize();
        for (int i = -5; i <= 5; i++) {
            Vector particleOffset = direction.clone().multiply(2).add(perpendicular.clone().multiply(i * 0.4));
            caster.getWorld().spawnParticle(Particle.SWEEP_ATTACK,
                caster.getLocation().add(particleOffset).add(0, 1, 0), 1, 0, 0, 0, 0);
        }
        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 1.0f, 0.8f);
    }

    private void playAftershockEffect(LivingEntity entity) {
        Location loc = entity.getLocation().add(0, 1, 0);
        loc.getWorld().spawnParticle(Particle.EXPLOSION, loc, 1, 0, 0, 0, 0);
        loc.getWorld().spawnParticle(Particle.DUST, loc, 8, 0.4, 0.4, 0.4, 0,
            new Particle.DustOptions(AFTERSHOCK_COLOR, 1.5f));
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.3f);
    }

    private void applyKnockback(LivingEntity target, Vector swingDirection) {
        Vector toTarget = target.getLocation().subtract(caster.getLocation()).toVector().normalize();
        Vector perpendicular = new Vector(-swingDirection.getZ(), 0, swingDirection.getX()).normalize();
        double dotProduct = toTarget.dot(perpendicular);
        Vector knockbackDirection = perpendicular.clone().multiply(dotProduct > 0 ? 1 : -1);
        double knockbackStrength = Spellbook.getRangedValue(data, caster, target, Attribute.ADVANTAGE_PHYSICAL, minKnockback, maxKnockback, "knockbackStrength");
        knockbackDirection.multiply(knockbackStrength);
        knockbackDirection.setY(0.2);
        target.setVelocity(target.getVelocity().add(knockbackDirection));
    }

    private void playHitEffect(LivingEntity target) {
        target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 8, 0.3, 0.3, 0.3, 0.1);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.7f, 1.1f);
    }
}
