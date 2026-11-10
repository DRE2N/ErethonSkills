package de.erethon.spellbook.spells.assassin.cutthroat;

import de.erethon.spellbook.Spellbook;
import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.combat.CombatSourceContext;
import de.erethon.spellbook.combat.BossControl;
import de.erethon.spellbook.spells.vfx.SkillSignatureVfx;

import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BladeDash extends CutthroatBaseSpell {


    // The Cutthroat dashes forward, dealing damage to all enemies in its path.
    // A confirmed cut applies the Cutthroat's single Wound state (Bleeding).
    // After dashing, the Cutthroat gains energy based on the number of enemies hit.

    private final double dashDistance = data.getDouble("distance", 6.0);
    private final double dashSpeedMultiplier = data.getDouble("speedMultiplier", 1.8);
    private final double sideDashStrength = data.getDouble("sideDashStrength", 1.5);
    private final double damageWidth = data.getDouble("damageWidth", 1.5);
    private final double energyPerTarget = data.getDouble("energyPerTarget", 5.0);
    private final int maximumEnergyReturn = data.getInt("maximumEnergyReturn", 20);
    private final int woundDurationMin = data.getInt("woundDurationMin", 5) * 20;
    private final int woundDurationMax = data.getInt("woundDurationMax", 10) * 20;

    private final EffectData woundEffectData = Spellbook.getEffectData("Bleeding");
    private final Set<LivingEntity> affected = new HashSet<>();

    public BladeDash(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    public boolean onCast() {
        if (!super.onCast()) {
            return false;
        }
        Location location = caster.getLocation();
        location.setPitch(-10);
        Vector direction = location.getDirection().normalize();
        Vector inputOffset = new Vector();
        // Add dash to the left/right based on player input
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
        Vector forwardDash = direction.clone().multiply(dashDistance);
        // Combine forward dash and side dash (inputOffset)
        Vector dashVector = forwardDash.add(inputOffset);
        Location startLocation = location.clone().add(0, 0.5, 0);
        RayTraceResult obstacle = caster.getWorld().rayTraceBlocks(startLocation, dashVector.clone().normalize(),
                dashVector.length());
        Location endLocation = obstacle == null ? startLocation.clone().add(dashVector)
                : obstacle.getHitPosition().toLocation(caster.getWorld()).subtract(dashVector.clone().normalize().multiply(0.4));
        Vector actualDash = endLocation.toVector().subtract(startLocation.toVector());

        playDashWindup(startLocation, endLocation, direction);
        new BukkitRunnable() {
            @Override
            public void run() {
                if (caster.isValid() && !caster.isDead()) {
                    executeDash(startLocation, endLocation, actualDash);
                }
            }
        }.runTaskLater(Spellbook.getInstance().getImplementer(), 4L);

        return true;
    }

    private void playDashWindup(Location startLocation, Location endLocation, Vector direction) {
        SkillSignatureVfx.accent(startLocation, getClass(), "lead", 2.4f, 13, true);
        caster.getWorld().playSound(startLocation, Sound.ENTITY_ENDERMAN_TELEPORT, 0.3f, 2.0f);
    }
    private void executeDash(Location startLocation, Location endLocation, Vector dashVector) {
        Set<LivingEntity> affectedTargets = new HashSet<>();
        BoundingBox dashPathBox = BoundingBox.of(startLocation, endLocation).expand(damageWidth / 2.0, 0.5, damageWidth / 2.0);

        for (Entity entity : caster.getWorld().getNearbyEntities(dashPathBox)) {
            if (entity instanceof LivingEntity living && !entity.equals(caster) && Spellbook.canAttack(caster, living)) {
                affected.add(living);
                living.getCollidableExemptions().add(caster.getUniqueId());

                double damage = Spellbook.getVariedAttributeBasedDamage(data, caster, living, true, Attribute.ADVANTAGE_PHYSICAL);
                double before = living.getHealth() + living.getAbsorptionAmount();
                CombatSourceContext.runSpell(this, () -> living.damage(damage, caster, PDamageType.PHYSICAL));
                if (living.getHealth() + living.getAbsorptionAmount() < before) {
                    BossControl.apply(caster, living, 15);
                    int woundDuration = (int) Spellbook.getRangedValue(data, caster, living,
                            Attribute.ADVANTAGE_MAGICAL, woundDurationMin, woundDurationMax, "woundDuration");
                    living.addEffect(caster, woundEffectData, woundDuration, 1);
                    affectedTargets.add(living);
                    playSlashEffect(living.getLocation());
                }
            }
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                for (LivingEntity target : affected) {
                    if (target.isValid()) {
                        target.getCollidableExemptions().remove(caster.getUniqueId());
                    }
                }
                affected.clear();
            }
        }.runTaskLater(Spellbook.getInstance().getImplementer(), 8L);

        int energyGain = Math.min(maximumEnergyReturn, (int) (affectedTargets.size() * energyPerTarget));
        caster.setEnergy(caster.getEnergy() + energyGain);

        Vector velocity = dashVector.clone().normalize().multiply(dashSpeedMultiplier);
        caster.setVelocity(velocity);

        createDashTrail(startLocation, endLocation);
        playDashSounds(startLocation);
        createDashImpact(endLocation, affectedTargets.size());
        triggerTraits(affectedTargets);
    }

    private void createDashTrail(Location start, Location end) {
        Vector direction = end.toVector().subtract(start.toVector());
        SkillSignatureVfx.accent(start.clone().add(direction.clone().multiply(0.5)),
                getClass(), "wake", 3.0f, 17, false, direction);
    }

    private void playSlashEffect(Location hitLocation) {
        SkillSignatureVfx.accent(hitLocation.clone().add(0, 1, 0), getClass(),
                "main", 2.2f, 16, false);
    }

    private void createDashImpact(Location endLocation, int targetsHit) {
        SkillSignatureVfx.accent(endLocation, getClass(),
                targetsHit > 0 ? "main" : "wake", 3.4f, 18, true);
    }
    private void playDashSounds(Location location) {
        location.getWorld().playSound(location, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.2f, 1.2f);
        location.getWorld().playSound(location, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.5f);
        location.getWorld().playSound(location, Sound.ITEM_TRIDENT_THROW, 0.6f, 1.8f);
    }

    @Override
    protected void addSpellPlaceholders() {
        spellAddedPlaceholders.add(Component.text(Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_MAGICAL,
                woundDurationMin, woundDurationMax, "woundDuration") / 20, VALUE_COLOR));
        placeholderNames.add("woundDuration");
    }

}
