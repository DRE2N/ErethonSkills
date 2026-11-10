package de.erethon.spellbook.traits.assassin.shadow;

import de.erethon.papyrus.PDamageType;
import de.erethon.papyrus.combat.CombatSource;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import de.erethon.spellbook.combat.BossControl;
import de.erethon.spellbook.spells.assassin.shadow.ShadowMark;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInputEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

/** Confirmed weapon-hit rewards, real rear-angle check, and one manual air jump. */
public class ShadowBasicAttack extends SpellTrait implements Listener {

    private final double rearConeDegrees = data.getDouble("backAngleForBonus", 40);
    private final int normalEnergy = data.getInt("normalEnergy", 10);
    private final int bonusEnergy = data.getInt("bonusEnergy", 10);
    private final double bonusDamageMultiplier = data.getDouble("bonusDamageMultiplier", 1.2);
    private final double doubleJumpMultiplier = data.getDouble("doubleJumpMultiplier", 0.25);
    private final double doubleJumpVelocity = data.getDouble("doubleJumpVelocity", 0.6);
    private final AttributeModifier noFallDamageModifier = new AttributeModifier(
            new NamespacedKey("spellbook", "shadow_basic_attack"), 16,
            AttributeModifier.Operation.ADD_NUMBER);
    private boolean usedAirJump;

    public ShadowBasicAttack(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onAdd() {
        super.onAdd();
        caster.setMaxEnergy(100);
        var safeFall = caster.getAttribute(Attribute.SAFE_FALL_DISTANCE);
        if (safeFall != null && safeFall.getModifier(noFallDamageModifier.getKey()) == null) {
            safeFall.addTransientModifier(noFallDamageModifier);
        }
        Bukkit.getPluginManager().registerEvents(this, Spellbook.getInstance().getImplementer());
    }

    @Override
    protected void onRemove() {
        HandlerList.unregisterAll(this);
        var safeFall = caster.getAttribute(Attribute.SAFE_FALL_DISTANCE);
        if (safeFall != null && safeFall.getModifier(noFallDamageModifier.getKey()) != null) {
            safeFall.removeModifier(noFallDamageModifier.getKey());
        }
        super.onRemove();
    }

    private boolean weaponHit(EntityDamageByEntityEvent event) {
        CombatSource source = event.getCombatSource();
        return event.getDamager() == caster && event.getEntity() instanceof LivingEntity
                && event.getDamageType() == PDamageType.PHYSICAL && source != null
                && source.type() == CombatSource.Type.BASIC_ATTACK;
    }

    private boolean rearAngle(LivingEntity target) {
        Vector facing = target.getLocation().getDirection().setY(0);
        Vector toCaster = caster.getLocation().toVector().subtract(target.getLocation().toVector()).setY(0);
        return facing.lengthSquared() > 0.01 && toCaster.lengthSquared() > 0.01
                && facing.normalize().dot(toCaster.normalize()) < -Math.cos(Math.toRadians(rearConeDegrees));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void amplifyRearHit(EntityDamageByEntityEvent event) {
        if (weaponHit(event) && event.getDamage() > 0 && rearAngle((LivingEntity) event.getEntity())) {
            event.setDamage(event.getDamage() * bonusDamageMultiplier);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void confirmWeaponHit(EntityDamageByEntityEvent event) {
        if (!weaponHit(event) || event.getFinalDamage() <= 0) return;
        LivingEntity target = (LivingEntity) event.getEntity();
        boolean flank = rearAngle(target);
        caster.addEnergy(normalEnergy + (flank ? bonusEnergy : 0));
        boolean marked = target.getTags().contains(ShadowMark.tag(caster));
        BossControl.apply(caster, target, marked ? 20 : flank ? 15 : 10);
        if (flank) {
            caster.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.7f, 1.55f);
            target.getWorld().spawnParticle(Particle.CRIT, target.getEyeLocation(), 8, 0.25, 0.3, 0.25, 0.08);
        }
    }

    @EventHandler
    public void resetAirJump(PlayerMoveEvent event) {
        if (event.getPlayer() == caster && caster.isOnGround()) usedAirJump = false;
    }

    @EventHandler
    public void onJump(PlayerInputEvent event) {
        if (event.getPlayer() != caster || event.getPlayer().isSneaking()
                || !event.getInput().isJump() || caster.isOnGround() || usedAirJump) return;
        Vector boost = caster.getEyeLocation().getDirection().setY(0);
        if (boost.lengthSquared() > 0.01) boost.normalize().multiply(doubleJumpMultiplier);
        boost.setY(doubleJumpVelocity);
        caster.setVelocity(caster.getVelocity().add(boost));
        usedAirJump = true;
        caster.getWorld().spawnParticle(Particle.CLOUD, caster.getLocation(), 5, 0.3, 0.1, 0.3, 0.02);
    }
}
