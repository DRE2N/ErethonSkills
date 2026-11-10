package de.erethon.spellbook.traits.assassin.sharpshooter;

import de.erethon.papyrus.PDamageType;
import de.erethon.papyrus.combat.CombatContext;
import de.erethon.papyrus.combat.CombatSource;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.combat.BossControl;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import de.slikey.effectlib.effect.LineEffect;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class SharpshooterBasicAttack extends SpellTrait implements Listener {

    // The Sharpshooter's basic attack is a charged shot that deals bonus damage based on the charge time.
    // Charging starts with left click, and is released with left click again or after a certain time.
    // The shot can hit a target up to 64 blocks away, and deals bonus damage based on the distance to the target.
    // Additionally, hitting the head of the target deals 50% more damage.

    private final int attackMinChargeTicks = data.getInt("attackMinChargeTicks", 20);
    private final int attackMaxChargeTicks = data.getInt("attackMaxChargeTicks", 60);
    private final double attackRaySize = data.getDouble("attackRaySize", 0.2);
    private final double attackRange = data.getDouble("attackRange", 64.0);
    private final double headshotRangeFromEye = data.getDouble("headshotRangeFromEye", 0.33);
    private final double headshotMultiplier = data.getDouble("headshotMultiplier", 1.5);
    private final double movementSpeedMultiplier = data.getDouble("movementSpeedMultiplier", 0.8);
    private final int focusPerSecond = data.getInt("focusPerSecond", 1);
    private final int focusPerBodyshot = data.getInt("focusPerBodyshot", 10);
    private final int focusPerHeadshot = data.getInt("focusPerHeadshot", 25);
    private final double ultChargedShotDamageBonus = data.getDouble("ultChargedShotDamageBonus", 0.2);

    private final AttributeModifier movementSpeedModifier = new AttributeModifier(NamespacedKey.fromString("spellbook:sharpshooter_basic_attack"), -movementSpeedMultiplier, AttributeModifier.Operation.MULTIPLY_SCALAR_1);

    private int currentChargeTicks = -1;
    private int focusTicks = 0;

    private net.minecraft.world.entity.player.Player nmsPlayer;
    private float oldFov;

    public SharpshooterBasicAttack(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @EventHandler
    private void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (player != caster || !event.getAction().isLeftClick() || event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (!caster.getPersistentDataContainer().has(Spellbook.PERSISTENT_CASTING_KEY)) {
            return;
        }
        if (currentChargeTicks == -1 && (event.getClickedBlock() != null
                || caster.getTargetEntity((int) caster.getAttribute(Attribute.ENTITY_INTERACTION_RANGE).getValue()) != null)) {
            return;
        }

        if (currentChargeTicks == -1) {
            currentChargeTicks = 0;
            caster.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, SoundCategory.RECORDS, 1f, 1.3f);
            if (caster.getAttribute(Attribute.MOVEMENT_SPEED).getModifier(movementSpeedModifier.getKey()) != null) {
                caster.getAttribute(Attribute.MOVEMENT_SPEED).removeModifier(movementSpeedModifier);
            }
            caster.getAttribute(Attribute.MOVEMENT_SPEED).addTransientModifier(movementSpeedModifier);
            oldFov = nmsPlayer.getAbilities().getWalkingSpeed();
            nmsPlayer.getAbilities().setWalkingSpeed(0.1f); // This is badly named and only affects the FOV, not the actual walking speed.
            return;
        }
        if (currentChargeTicks < attackMinChargeTicks) {
            currentChargeTicks = -1;
            caster.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, SoundCategory.RECORDS, 1f, 0.5f);
            caster.getAttribute(Attribute.MOVEMENT_SPEED).removeModifier(movementSpeedModifier);
            nmsPlayer.getAbilities().setWalkingSpeed(oldFov);
            return;
        }
        releaseShot();
    }

    @Override
    protected void onTick() {
        if (!caster.getPersistentDataContainer().has(Spellbook.PERSISTENT_CASTING_KEY)) {
            return;
        }
        // Focus regeneration
        if (focusTicks >= 20) {
            focusTicks = 0;
            caster.addEnergy(focusPerSecond);
        } else {
            focusTicks++;
        }
        // Shooting mechanic
        if (currentChargeTicks >= 0) {
            currentChargeTicks++;

            if (currentChargeTicks >= attackMaxChargeTicks) {
                releaseShot();
                return;
            }
            if (currentChargeTicks % 5 != 0) {
                return;
            }
            Location eye = caster.getEyeLocation();
            LineEffect lineEffect = new LineEffect(Spellbook.getInstance().getEffectManager());
            lineEffect.setLocation(eye.clone().add(eye.getDirection().multiply(0.5)));
            Vector direction = caster.getEyeLocation().getDirection().normalize();
            direction = direction.multiply((attackRange * (currentChargeTicks / (double) attackMaxChargeTicks)));
            lineEffect.setTarget(eye.clone().add(direction));
            lineEffect.duration = 5;
            lineEffect.particle = Particle.DUST;
            lineEffect.color = Color.AQUA;
            lineEffect.particleData = 0;
            lineEffect.particles = 8;
            lineEffect.start();
            if (currentChargeTicks % 20 == 0) {
                caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME,
                        SoundCategory.PLAYERS, 0.7f, 0.9f + currentChargeTicks / 80.0f);
            }
        }
    }

    private void releaseShot() {
        if (!caster.getPersistentDataContainer().has(Spellbook.PERSISTENT_CASTING_KEY)) {
            return;
        }
        caster.getAttribute(Attribute.MOVEMENT_SPEED).removeModifier(movementSpeedModifier);
        nmsPlayer.getAbilities().setWalkingSpeed(oldFov);
        Location shotOrigin = caster.getEyeLocation();
        Vector shotDirection = shotOrigin.getDirection().normalize();
        RayTraceResult ray = caster.getWorld().rayTraceEntities(shotOrigin, shotDirection, attackRange,
                attackRaySize, entity -> entity instanceof LivingEntity living
                        && living != caster && Spellbook.canAttack(caster, living));
        RayTraceResult blockRay = caster.getWorld().rayTraceBlocks(shotOrigin, shotDirection,
                attackRange, FluidCollisionMode.NEVER, true);
        if (ray != null && blockRay != null && blockRay.getHitPosition().distanceSquared(shotOrigin.toVector())
                < ray.getHitPosition().distanceSquared(shotOrigin.toVector())) {
            ray = null;
        }
        if (ray == null || ray.getHitEntity() == null || !(ray.getHitEntity() instanceof LivingEntity target) || target == caster || !Spellbook.canAttack(caster, target)) {
            currentChargeTicks = -1;
            caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_RESIN_BREAK, SoundCategory.RECORDS, 1f, 1.0f);
            return;
        }
        double baseDamage = Spellbook.getVariedAttributeBasedDamage(data, caster, target, true, Attribute.ADVANTAGE_MAGICAL);
        double chargeFraction = Math.min(1.0, currentChargeTicks / (double) attackMaxChargeTicks);
        double damage = baseDamage * (1.0 + chargeFraction);
        Location location = target.getEyeLocation();
        BoundingBox boundingBox = new BoundingBox(location.getX() - headshotRangeFromEye, location.getY() - headshotRangeFromEye, location.getZ() - headshotRangeFromEye,
                location.getX() + headshotRangeFromEye, location.getY() + headshotRangeFromEye, location.getZ() + headshotRangeFromEye);
        boolean headshot = boundingBox.rayTrace(shotOrigin.toVector(), shotOrigin.getDirection(), attackRange) != null;
        if (headshot) {
            damage *= headshotMultiplier;
        }
        if (target.getTags().contains("singular_weakness_" + caster.getUniqueId())) {
            damage *= (1 + ultChargedShotDamageBonus);
        }
        double before = target.getHealth() + target.getAbsorptionAmount();
        try (CombatContext.Scope ignored = CombatContext.enter(CombatSource.trait(this))) {
            target.damage(damage, caster, PDamageType.MAGIC);
        }
        if (target.getHealth() + target.getAbsorptionAmount() < before) {
            BossControl.apply(caster, target, 10 + 5 * chargeFraction + (headshot ? 5 : 0));
            caster.addEnergy(headshot ? focusPerHeadshot : focusPerBodyshot);
            caster.getWorld().playSound(caster.getLocation(), headshot
                    ? Sound.BLOCK_ANVIL_HIT : Sound.ENTITY_ARROW_HIT_PLAYER,
                    SoundCategory.PLAYERS, headshot ? 1.0f : 0.65f, headshot ? 1.2f : 1.5f);
            target.getWorld().spawnParticle(headshot ? Particle.CRIT : Particle.ENCHANTED_HIT,
                    target.getEyeLocation(), headshot ? 12 : 5, 0.2, 0.2, 0.2, 0.08);
        }
        currentChargeTicks = -1;

}

    @Override
    protected void onAdd() {
        super.onAdd();
        Bukkit.getPluginManager().registerEvents(this, Spellbook.getInstance().getImplementer());
        CraftPlayer craftPlayer = (CraftPlayer) caster;
        nmsPlayer = craftPlayer.getHandle();
        craftPlayer.setMaxEnergy(100);
    }

    @Override
    protected void onRemove() {
        super.onRemove();
        HandlerList.unregisterAll(this);
        caster.getAttribute(Attribute.MOVEMENT_SPEED).removeModifier(movementSpeedModifier);
        nmsPlayer.getAbilities().setWalkingSpeed(oldFov);
    }
}
