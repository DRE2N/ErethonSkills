package de.erethon.spellbook.spells.assassin.shadow;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.aoe.AoE;
import de.erethon.spellbook.spells.assassin.AssassinBaseSpell;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Input;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.Vector;

public class ShadowJump extends ShadowBaseSpell implements Listener {

    // Dash forward quickly, passing through enemies. If you pass through an enemy, you become invisible for a short duration.
    // You can influence your dash direction slightly by moving left or right.

    private final int durationTicks = data.getInt("durationTicks", 40);
    private final double dashMultiplier = data.getDouble("dashMultiplier", 2.2);
    private final double sideDashStrength = data.getDouble("sideDashStrength", 1.5);
    private final int invisibilityDuration = data.getInt("invisibilityDuration", 6) * 20;

    private boolean invisible = false;
    private boolean hasPassedThroughEntity = false;
    private AoE shadowTrail = null;
    private Location startLocation = null;

    public ShadowJump(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = durationTicks;
        tickInterval = 1;
    }

    @Override
    public boolean onCast() {
        startLocation = caster.getLocation().clone();
        Location location = caster.getLocation();
        location.setPitch(-10);
        Vector direction = location.getDirection().normalize();
        Vector inputOffset = new Vector();

        if (caster instanceof Player player) {
            Input input = player.getCurrentInput();
            Vector right = direction.clone().crossProduct(new Vector(0, 1, 0)).normalize();
            if (input.isLeft()) {
                inputOffset.add(right.clone().multiply(-sideDashStrength));
            }
            if (input.isRight()) {
                inputOffset.add(right.clone().multiply(sideDashStrength));
            }
        }

        Vector forwardDash = direction.multiply(dashMultiplier);
        Vector dashVector = forwardDash.add(inputOffset);
        caster.setVelocity(dashVector);

        caster.getWorld().playSound(startLocation, Sound.ENTITY_PHANTOM_FLAP, SoundCategory.RECORDS, 0.8f, 1.5f);
        caster.getWorld().playSound(startLocation, Sound.BLOCK_SOUL_SAND_BREAK, SoundCategory.RECORDS, 0.6f, 1.8f);

        caster.getWorld().spawnParticle(Particle.DUST, startLocation.add(0, 1, 0), 20, 1, 1, 1, 0.1, new Particle.DustOptions(Color.BLACK, 1.5f));
        caster.getWorld().spawnParticle(Particle.SMOKE, startLocation.add(0, 0.5, 0), 12, 0.8, 0.3, 0.8, 0.1);

        shadowTrail = createCircularAoE(startLocation, 1.5, 0.5, 60)
            .addBlockChange(Material.GRAY_CONCRETE_POWDER)
            .sendBlockChanges();

        triggerTraits(0);
        Bukkit.getPluginManager().registerEvents(this, Spellbook.getInstance().getImplementer());
        return super.onCast();
    }

    @Override
    protected void onTick() {
        if (invisible) {
            caster.getWorld().spawnParticle(Particle.DUST, caster.getLocation().add(0, 1, 0), 2, 0.3, 0.3, 0.3, 0, new Particle.DustOptions(Color.fromRGB(96, 96, 96), 0.6f));
            return;
        }

        caster.getWorld().spawnParticle(Particle.WHITE_ASH, caster.getLocation().add(0, 0.8, 0), 2, 0.2, 0.2, 0.2, 0.02);
        caster.getWorld().spawnParticle(Particle.DUST, caster.getLocation(), 1, 0.1, 0.1, 0.1, 0, new Particle.DustOptions(Color.fromRGB(64, 64, 64), 0.8f));

        for (LivingEntity entity : caster.getLocation().getNearbyLivingEntities(2.5)) {
            if (entity == caster || !Spellbook.canAttack(caster, entity)) {
                continue;
            }
            if (entity.getBoundingBox().overlaps(caster.getBoundingBox()) ||
                caster.getLocation().distance(entity.getLocation()) < 1.5) {

                activateInvisibility();
                hasPassedThroughEntity = true;
                break;
            }
        }
    }

    private void activateInvisibility() {
        if (invisible) return;

        invisible = true;
        caster.setInvisible(true);
        keepAliveTicks = Math.max(keepAliveTicks, invisibilityDuration);

        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_PHANTOM_DEATH, SoundCategory.RECORDS, 0.5f, 1.8f);
        caster.getWorld().spawnParticle(Particle.DUST, caster.getLocation().add(0, 1, 0), 15, 0.8, 0.8, 0.8, 0.1, new Particle.DustOptions(Color.BLACK, 1.2f));
        caster.getWorld().spawnParticle(Particle.SMOKE, caster.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.1);

        if (hasPassedThroughEntity) {
            triggerTraits(1);
        }
        // Clear mob targets
        for (LivingEntity entity : caster.getLocation().getNearbyLivingEntities(16)) {
            if (Spellbook.canAttack(caster, entity) && entity != caster && entity instanceof Mob mob) {
                if (mob.getTarget() == caster) {
                    mob.setTarget(null);
                }
            }
        }
        for (Player player : caster.getTrackedBy()) {
            player.sendEquipmentChange(caster, EquipmentSlot.HAND, null);
            player.sendEquipmentChange(caster, EquipmentSlot.OFF_HAND, null);
            player.sendEquipmentChange(caster, EquipmentSlot.HEAD, null);
            player.sendEquipmentChange(caster, EquipmentSlot.CHEST, null);
            player.sendEquipmentChange(caster, EquipmentSlot.LEGS, null);
            player.sendEquipmentChange(caster, EquipmentSlot.FEET, null);
        }
    }


    @EventHandler
    private void onTarget(EntityTargetEvent event) {
        if (event.getTarget() == null || event.getTarget() != caster) {
            return;
        }
        if (!caster.getTags().contains("shadow_cloak")) {
            return;
        }
        if (event.getReason() == EntityTargetEvent.TargetReason.TARGET_INVALID || event.getReason() == EntityTargetEvent.TargetReason.FORGOT_TARGET) {
            return;
        }
        event.setCancelled(true);
    }

    @Override
    protected void onTickFinish() {
        caster.setInvisible(false);
        if (shadowTrail != null) {
            shadowTrail.revertBlockChanges();
        }

        if (invisible) {
            caster.getWorld().spawnParticle(Particle.DUST, caster.getLocation().add(0, 1, 0), 12, 0.6, 0.6, 0.6, 0.1, new Particle.DustOptions(Color.fromRGB(128, 128, 128), 1.0f));
            caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, SoundCategory.RECORDS, 0.4f, 2.0f);
            // Restore equipment visibility
            for (Player player : caster.getTrackedBy()) {
                for (EquipmentSlot slot : EquipmentSlot.values()) {
                    if (caster.getEquipment() != null) {
                        caster.getEquipment().getItem(slot);
                        if (caster.getEquipment().getItem(slot).getType() != Material.AIR) {
                            player.sendEquipmentChange(caster, slot, caster.getEquipment().getItem(slot));
                        }
                    }
                }
            }
        }
    }

}
