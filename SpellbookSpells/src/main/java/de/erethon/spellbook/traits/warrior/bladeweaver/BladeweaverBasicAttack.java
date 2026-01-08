package de.erethon.spellbook.traits.warrior.bladeweaver;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Bladeweaver Basic Attack Trait
 *
 * Each basic attack grants energy (ult charge) and temporary bonus health.
 * The bonus health decays over time, encouraging aggressive play.
 */
public class BladeweaverBasicAttack extends SpellTrait {

    private final int energyPerAttack = data.getInt("energyPerAttack", 5);
    private final double bonusHealthPercentPerHit = data.getDouble("bonusHealthPercentPerHit", 1.0); // 1% per hit
    private final double maxBonusHealthPercent = data.getDouble("maxBonusHealthPercent", 10.0); // 10% max
    private final int bonusHealthDuration = data.getInt("bonusHealthDurationTicks", 100); // 5 seconds
    private final int bonusHealthDecayInterval = data.getInt("bonusHealthDecayIntervalTicks", 20); // Every second
    private final double bonusHealthDecayPercent = data.getDouble("bonusHealthDecayPercent", 1.0); // 1% per decay

    private static final NamespacedKey BONUS_HEALTH_KEY = NamespacedKey.fromString("spellbook:bladeweaver_bonus_health");

    private double currentBonusHealth = 0;
    private AttributeModifier currentModifier = null;
    private BukkitRunnable decayTask = null;

    public BladeweaverBasicAttack(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    public double onAttack(LivingEntity target, double damage, PDamageType type) {
        if (!Spellbook.canAttack(caster, target)) {
            return super.onAttack(target, damage, type);
        }

        // Grant energy
        caster.addEnergy(energyPerAttack);

        // Grant bonus health (calculate from percentage)
        double baseMaxHealth = getBaseMaxHealth();
        double bonusHealthAmount = baseMaxHealth * (bonusHealthPercentPerHit / 100.0);
        addBonusHealth(bonusHealthAmount);

        return super.onAttack(target, damage, type);
    }

    /**
     * Gets the caster's base max health without any modifiers
     */
    private double getBaseMaxHealth() {
        return caster.getAttribute(Attribute.MAX_HEALTH).getBaseValue();
    }

    /**
     * Adds bonus health to the caster. Can be called from abilities too.
     */
    public void addBonusHealth(double amount) {
        double baseMaxHealth = getBaseMaxHealth();
        double maxBonus = baseMaxHealth * (maxBonusHealthPercent / 100.0);
        double newBonusHealth = Math.min(currentBonusHealth + amount, maxBonus);

        if (newBonusHealth != currentBonusHealth) {
            currentBonusHealth = newBonusHealth;
            updateHealthModifier();

            // Visual feedback
            playBonusHealthEffect();

            // Start or reset decay timer
            startDecayTimer();
        }
    }

    private void updateHealthModifier() {
        // Remove old modifier if exists
        if (currentModifier != null) {
            caster.getAttribute(Attribute.MAX_HEALTH).removeModifier(currentModifier);
        }

        if (currentBonusHealth > 0) {
            // Create new modifier with current bonus health
            currentModifier = new AttributeModifier(
                BONUS_HEALTH_KEY,
                currentBonusHealth,
                AttributeModifier.Operation.ADD_NUMBER
            );
            caster.getAttribute(Attribute.MAX_HEALTH).addTransientModifier(currentModifier);

            // Also heal for the amount gained (so the bonus health is actually usable)
            double currentHealth = caster.getHealth();
            double maxHealth = caster.getAttribute(Attribute.MAX_HEALTH).getValue();
            caster.setHealth(Math.min(currentHealth + currentBonusHealth, maxHealth));
        }
    }

    private void playBonusHealthEffect() {
        // Orange/gold particles for Bladeweaver theme
        caster.getWorld().spawnParticle(Particle.DUST,
            caster.getLocation().add(0, 1, 0),
            5, 0.3, 0.5, 0.3, 0,
            new Particle.DustOptions(org.bukkit.Color.fromRGB(255, 165, 0), 1.0f));
        caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_CHAIN_HIT, 0.3f, 1.5f);
    }

    private void startDecayTimer() {
        // Cancel existing decay task
        if (decayTask != null) {
            decayTask.cancel();
        }

        // Start new decay task
        decayTask = new BukkitRunnable() {
            private int ticksRemaining = bonusHealthDuration;

            @Override
            public void run() {
                ticksRemaining -= bonusHealthDecayInterval;

                if (ticksRemaining <= 0 || currentBonusHealth <= 0) {
                    // Remove all bonus health
                    removeBonusHealth();
                    this.cancel();
                    return;
                }

                // Decay bonus health by percentage
                double baseMaxHealth = getBaseMaxHealth();
                double decayAmount = baseMaxHealth * (bonusHealthDecayPercent / 100.0);
                currentBonusHealth = Math.max(0, currentBonusHealth - decayAmount);
                updateHealthModifier();

                if (currentBonusHealth <= 0) {
                    this.cancel();
                }
            }
        };
        decayTask.runTaskTimer(Spellbook.getInstance().getImplementer(), bonusHealthDecayInterval, bonusHealthDecayInterval);
    }

    private void removeBonusHealth() {
        if (currentModifier != null) {
            caster.getAttribute(Attribute.MAX_HEALTH).removeModifier(currentModifier);
            currentModifier = null;
        }
        currentBonusHealth = 0;
    }

    @Override
    protected void onAdd() {
        caster.setMaxEnergy(100);
    }

    @Override
    protected void onRemove() {
        removeBonusHealth();
        if (decayTask != null) {
            decayTask.cancel();
            decayTask = null;
        }
    }
}
