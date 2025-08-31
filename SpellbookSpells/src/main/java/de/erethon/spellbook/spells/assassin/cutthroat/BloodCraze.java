package de.erethon.spellbook.spells.assassin.cutthroat;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.aoe.AoE;
import de.erethon.spellbook.api.SpellData;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.CircleEffect;
import de.slikey.effectlib.effect.SphereEffect;
import de.erethon.spellbook.spells.assassin.AssassinBaseSpell;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;

public class BloodCraze extends AssassinBaseSpell {

    // The Cutthroat enters a blood frenzy, increasing their movement speed and dealing cleaving damage to nearby enemies.
    // The Cutthroat heals for a portion of the damage dealt. The healing scales with resistance_magical.

    private final double cleavingRange = data.getDouble("cleavingRange", 3.0);
    private final double cleavingDamageMultiplier = data.getDouble("cleavingDamageMultiplier", 0.8);
    private final double damageAsHealingMultiplierMin = data.getDouble("damageAsHealingMultiplierMin", 0.15);
    private final double damageAsHealingMultiplierMax = data.getDouble("damageAsHealingMultiplierMax", 0.25);
    private final double bloodAuraRadius = data.getDouble("bloodAuraRadius", 4.0);

    private final AttributeModifier speedBoost = new AttributeModifier(new NamespacedKey("spellbook", "blood_craze"), 0.2, AttributeModifier.Operation.ADD_NUMBER);

    private int visualTicks = 20;
    private int auraUpdateTicks = 40;

    public BloodCraze(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = duration * 20;
    }

    @Override
    protected boolean onPrecast() {
        if (caster.getTags().contains("assassin.blood_craze")) {
            return false;
        }
        return super.onPrecast();
    }

    @Override
    public boolean onCast() {
        caster.getAttribute(Attribute.MOVEMENT_SPEED).addTransientModifier(speedBoost);

        playBloodCrazeActivation();
        createBloodAura();
        caster.getTags().add("assassin.blood_craze");

        return super.onCast();
    }

    private void playBloodCrazeActivation() {
        Location casterLoc = caster.getLocation().add(0, 1, 0);

        caster.getWorld().spawnParticle(Particle.BLOCK, casterLoc, 30, 1.0, 1.0, 1.0, 0.1, Material.REDSTONE_BLOCK.createBlockData());
        caster.getWorld().spawnParticle(Particle.DUST, casterLoc, 20, 0.8, 0.8, 0.8, 0.1, new Particle.DustOptions(org.bukkit.Color.MAROON, 2.0f));

        caster.getWorld().playSound(casterLoc, Sound.ENTITY_WITHER_HURT, 0.6f, 1.5f);
        caster.getWorld().playSound(casterLoc, Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 0.4f, 1.2f);

        EffectManager effectManager = Spellbook.getInstance().getEffectManager();
        if (effectManager != null) {
            SphereEffect bloodBurst = new SphereEffect(effectManager);
            bloodBurst.setLocation(casterLoc);
            bloodBurst.radius = 2.0f;
            bloodBurst.particle = Particle.DUST;
            bloodBurst.particles = 40;
            bloodBurst.duration = 15;
            bloodBurst.start();
        }
    }

    private void createBloodAura() {
        AoE bloodAura = createCircularAoE(caster.getLocation(), bloodAuraRadius, 1.0, keepAliveTicks)
                .followEntity(caster)
                .onTick(aoe -> {
                    if (currentTicks % 20 == 0) {
                        Location auraCenter = caster.getLocation().add(0, 0.5, 0);
                        caster.getWorld().spawnParticle(Particle.DUST, auraCenter, 8, bloodAuraRadius * 0.7, 0.2, bloodAuraRadius * 0.7, 0,
                            new Particle.DustOptions(org.bukkit.Color.RED, 1.2f));
                    }
                });

        bloodAura.addDisplay(bloodAura.createDisplay()
                .blockDisplay(Material.REDSTONE_WIRE)
                .scale(0.8f)
                .translate(0, 0.05f, 0)
                .build())
                .sendBlockChanges();
    }

    @Override
    protected void onTickFinish() {
        super.onTickFinish();
        caster.getAttribute(Attribute.MOVEMENT_SPEED).removeModifier(speedBoost);

        playBloodCrazeEnd();
    }

    private void playBloodCrazeEnd() {
        Location casterLoc = caster.getLocation().add(0, 1, 0);

        caster.getWorld().spawnParticle(Particle.SMOKE, casterLoc, 15, 0.5, 0.5, 0.5, 0.05);
        caster.getWorld().playSound(casterLoc, Sound.BLOCK_FIRE_EXTINGUISH, 0.3f, 1.2f);
    }

    @Override
    protected void onTick() {
        super.onTick();
        visualTicks--;
        auraUpdateTicks--;

        if (visualTicks <= 0) {
            visualTicks = 20;
            Location casterLoc = caster.getLocation().add(0, 1, 0);
            caster.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, casterLoc, 3, 0.5, 0.5, 0.5);
            caster.getWorld().spawnParticle(Particle.DUST, casterLoc, 5, 0.3, 0.3, 0.3, 0,
                new Particle.DustOptions(org.bukkit.Color.MAROON, 1.0f));
        }

        if (auraUpdateTicks <= 0) {
            auraUpdateTicks = 40;
            createPulsingBloodEffect();
        }
    }

    private void createPulsingBloodEffect() {
        EffectManager effectManager = Spellbook.getInstance().getEffectManager();
        if (effectManager != null) {
            CircleEffect bloodPulse = new CircleEffect(effectManager);
            bloodPulse.setLocation(caster.getLocation().add(0, 0.1, 0));
            bloodPulse.radius = (float) bloodAuraRadius;
            bloodPulse.particle = Particle.DUST;
            bloodPulse.particles = 20;
            bloodPulse.duration = 8;
            bloodPulse.start();
        }
    }

    @Override
    public double onAttack(LivingEntity target, double damage, PDamageType type) {
        double cleavingDamage = damage * cleavingDamageMultiplier;
        double healingMultiplier = Spellbook.getRangedValue(data, caster, target, Attribute.RESISTANCE_MAGICAL, damageAsHealingMultiplierMin, damageAsHealingMultiplierMax, "healing");
        double healingAmount = damage * healingMultiplier;
        caster.heal(healingAmount);

        playHealingEffect();

        for (LivingEntity entity : target.getLocation().getNearbyLivingEntities(cleavingRange)) {
            if (entity != target && entity != caster && Spellbook.canAttack(caster, entity)) {
                entity.damage(cleavingDamage, caster);
                playCleavingEffect(entity);
            }
        }
        return super.onAttack(target, damage, type);
    }

    private void playHealingEffect() {
        Location casterLoc = caster.getLocation().add(0, 1, 0);
        caster.getWorld().spawnParticle(Particle.HEART, casterLoc, 3, 0.3, 0.3, 0.3, 0.1);
        caster.getWorld().spawnParticle(Particle.DUST, casterLoc, 8, 0.4, 0.4, 0.4, 0,
            new Particle.DustOptions(org.bukkit.Color.RED, 1.5f));
    }

    private void playCleavingEffect(LivingEntity entity) {
        Location entityLoc = entity.getLocation().add(0, 1, 0);
        entity.getWorld().spawnParticle(Particle.CRIT, entityLoc, 5, 0.3, 0.3, 0.3, 0.1);
        entity.getWorld().spawnParticle(Particle.DUST, entityLoc, 3, 0.2, 0.2, 0.2, 0,
            new Particle.DustOptions(org.bukkit.Color.MAROON, 1.0f));
        entity.getWorld().playSound(entityLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.4f, 1.3f);
    }

    @Override
    protected void addSpellPlaceholders() {
        spellAddedPlaceholders.add(Component.text(Spellbook.getRangedValue(data, caster, Attribute.RESISTANCE_MAGICAL, damageAsHealingMultiplierMin, damageAsHealingMultiplierMax, "healing"), VALUE_COLOR));
        placeholderNames.add("healing");
    }
}
