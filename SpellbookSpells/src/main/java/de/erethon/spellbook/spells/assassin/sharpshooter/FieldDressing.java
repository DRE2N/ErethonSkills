package de.erethon.spellbook.spells.assassin.sharpshooter;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.combat.HealingContext;
import de.erethon.spellbook.spells.vfx.SkillSignatureVfx;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.RayTraceResult;

/**
 * A deliberate self-or-ally treatment. Sneaking spends Focus for a stronger treatment.
 * The same base heal and cooldown apply to either target.
 */
public class FieldDressing extends SharpshooterBaseSpell implements Listener {

    private static final NamespacedKey SLOW_KEY = new NamespacedKey("spellbook", "field_dressing_slow");
    private final double baseHealing = data.getDouble("baseHealing", 14);
    private final double healingPowerCoefficient = data.getDouble("healingPowerCoefficient", 0.35);
    private final double focusHealingPerPoint = data.getDouble("focusHealingPerPoint", 0.4);
    private final int maximumFocusSpend = data.getInt("maximumFocusSpend", 20);
    private final int allyRange = data.getInt("allyRange", 8);
    private final double playerCombatHealingMultiplier = data.getDouble("playerCombatHealingMultiplier", 0.65);
    private final AttributeModifier slow = new AttributeModifier(SLOW_KEY, -0.35,
            AttributeModifier.Operation.MULTIPLY_SCALAR_1);

    private LivingEntity treatmentTarget;
    private double healingPerPulse;
    private double laterPulseMultiplier = 1.0;
    private int pulsesDelivered;

    public FieldDressing(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = 42;
    }

    @Override
    protected boolean onPrecast() {
        if (!super.onPrecast()) {
            return false;
        }
        treatmentTarget = caster;
        Location eye = caster.getEyeLocation();
        RayTraceResult allyRay = caster.getWorld().rayTraceEntities(eye, eye.getDirection(), allyRange,
                0.45, entity -> entity instanceof LivingEntity living && living != caster
                        && Spellbook.getInstance().getTeamManager().getTeam(caster) != null
                        && Spellbook.getInstance().getTeamManager().isInSameTeam(caster, living));
        RayTraceResult blockRay = caster.getWorld().rayTraceBlocks(eye, eye.getDirection(), allyRange,
                FluidCollisionMode.NEVER, true);
        if (allyRay != null && allyRay.getHitEntity() instanceof LivingEntity ally
                && (blockRay == null || blockRay.getHitPosition().distanceSquared(eye.toVector())
                > allyRay.getHitPosition().distanceSquared(eye.toVector()))) {
            treatmentTarget = ally;
        }
        return true;
    }

    @Override
    public boolean onCast() {
        if (!super.onCast()) {
            return false;
        }
        double healingPower = Math.min(80, Math.max(0,
                caster.getAttribute(Attribute.STAT_HEALINGPOWER).getValue()));
        int focusSpent = 0;
        if (caster instanceof Player player && player.isSneaking()) {
            focusSpent = Math.min(maximumFocusSpend, Math.max(0, caster.getEnergy()));
            caster.setEnergy(caster.getEnergy() - focusSpent);
        }
        double totalHealing = baseHealing + healingPowerCoefficient * healingPower
                + focusSpent * focusHealingPerPoint;
        totalHealing = Math.min(treatmentTarget.getMaxHealth() * 0.3, totalHealing);
        healingPerPulse = totalHealing / 4.0;
        laterPulseMultiplier = 1.0;
        pulsesDelivered = 0;
        if (treatmentTarget == caster) {
            var speed = caster.getAttribute(Attribute.MOVEMENT_SPEED);
            if (speed != null && speed.getModifier(SLOW_KEY) == null) {
                speed.addTransientModifier(slow);
            }
        }
        Bukkit.getPluginManager().registerEvents(this, Spellbook.getInstance().getImplementer());
        caster.getWorld().playSound(caster.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 0.8f, 1.1f);
        SkillSignatureVfx.accent(treatmentTarget.getLocation(), getClass(),
                "lead", 2.8f, 20, true);
        return true;
    }

    @Override
    protected void onTick() {
        super.onTick();
        if (currentTicks % 10 != 0 || pulsesDelivered >= 4 || treatmentTarget == null
                || !treatmentTarget.isValid() || treatmentTarget.isDead()) {
            return;
        }
        double amount = healingPerPulse * laterPulseMultiplier;
        if (HealingContext.isPlayerCombat(caster, treatmentTarget)) {
            amount *= playerCombatHealingMultiplier;
        }
        pulsesDelivered++;
        treatmentTarget.heal(amount);
        SkillSignatureVfx.accent(treatmentTarget.getLocation().add(0, 1, 0), getClass(),
                "wake", 1.3f, 12, false);
        treatmentTarget.getWorld().playSound(treatmentTarget.getLocation(),
                Sound.BLOCK_BREWING_STAND_BREW, 0.35f, 1.6f);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTreatmentInterrupted(EntityDamageEvent event) {
        if (event.getEntity() == treatmentTarget && event.getFinalDamage() > 0) {
            laterPulseMultiplier = Math.max(0.35, laterPulseMultiplier * 0.6);
        }
    }

    @Override
    protected void cleanup() {
        HandlerList.unregisterAll(this);
        if (treatmentTarget == caster) {
            var speed = caster.getAttribute(Attribute.MOVEMENT_SPEED);
            if (speed != null && speed.getModifier(SLOW_KEY) != null) {
                speed.removeModifier(SLOW_KEY);
            }
        }
        super.cleanup();
    }
}
