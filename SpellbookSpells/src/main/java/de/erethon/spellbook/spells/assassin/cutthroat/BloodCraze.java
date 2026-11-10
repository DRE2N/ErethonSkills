package de.erethon.spellbook.spells.assassin.cutthroat;

import de.erethon.papyrus.combat.CombatSource;
import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.combat.CombatSourceContext;
import de.erethon.spellbook.combat.HealingContext;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.vfx.SkillSignatureVfx;
import de.erethon.spellbook.spells.vfx.TerrainRing;
import de.erethon.spellbook.utils.TargetedParticles;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class BloodCraze extends CutthroatBaseSpell implements Listener {

    // The Cutthroat enters a blood frenzy, increasing their movement speed and dealing cleaving damage to nearby enemies.
    // The Cutthroat heals for a portion of the damage dealt. The healing scales with resistance_magical.
    // Aura: While Blood Craze is active, the Cutthroat emits a blood aura that damages nearby enemies every second.

    private final double cleavingRange = data.getDouble("cleavingRange", 3.0);
    private final double cleavingDamageMultiplier = data.getDouble("cleavingDamageMultiplier", 0.8);
    private final double castHealBase = data.getDouble("castHealBase", 8.0);
    private final double hitHealBase = data.getDouble("hitHealBase", 1.5);
    private final double playerHealingMultiplier = data.getDouble("playerHealingMultiplier", 0.55);
    private final double bloodAuraRadius = data.getDouble("bloodAuraRadius", 4.0);
    private final double speedDurationMin = data.getDouble("speedDurationMin", 5.0) * 20;
    private final double speedDurationMax = data.getDouble("speedDurationMax", 8.0) * 20;

    private final EffectData speedBoostData = Spellbook.getEffectData("Speed");

    private int visualTicks = 20;
    private int auraUpdateTicks = 40;
    private boolean dealingCleaveDamage;
    private double remainingHitHealing;
    private TerrainRing terrainRing;

    public BloodCraze(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = (int) Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_MAGICAL, speedDurationMin, speedDurationMax, "speedDuration");
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
        if (!super.onCast()) {
            return false;
        }
        int speedDuration = (int) Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_MAGICAL, speedDurationMin, speedDurationMax, "speedDuration");
        caster.addEffect(caster, speedBoostData, speedDuration, 1);
        double healingPower = Math.min(80, Math.max(0, caster.getAttribute(Attribute.STAT_HEALINGPOWER).getValue()));
        double castHealing = Math.min(caster.getMaxHealth() * 0.2, castHealBase + 0.3 * healingPower);
        caster.heal(HealingContext.isPlayerCombat(caster) ? castHealing * playerHealingMultiplier : castHealing);
        remainingHitHealing = Math.min(caster.getMaxHealth() * 0.25, 12 + 0.5 * healingPower);
        playHealingEffect();
        playBloodCrazeActivation();
        createBloodAura();
        caster.getTags().add("assassin.blood_craze");
        Bukkit.getPluginManager().registerEvents(this, Spellbook.getInstance().getImplementer());

        return true;
    }

    private void playBloodCrazeActivation() {
        Location casterLoc = caster.getLocation().add(0, 1, 0);
        SkillSignatureVfx.accent(caster.getLocation(), getClass(), "main", 7.0f, 28, true);
        SkillSignatureVfx.accent(casterLoc, getClass(), "wake", 3.0f, 20, false);
        caster.getWorld().playSound(casterLoc, Sound.ENTITY_WITHER_HURT, 0.6f, 1.5f);
    }

    private void createBloodAura() {
        terrainRing = new TerrainRing(caster.getLocation(), bloodAuraRadius,
                "terrain_band_cutthroat", 48, 0.4f);
    }

    @Override
    protected void onTickFinish() {
        super.onTickFinish();

        playBloodCrazeEnd();
    }

    @Override
    protected void cleanup() {
        if (terrainRing != null) terrainRing.close();
        super.cleanup();
        HandlerList.unregisterAll(this);
        caster.getTags().remove("assassin.blood_craze");
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
            SkillSignatureVfx.accent(caster.getLocation(), getClass(), "wake", 4.5f, 20, true);
        }

        if (auraUpdateTicks <= 0) {
            auraUpdateTicks = 40;
            createPulsingBloodEffect();
        }
        animateBloodAura();
    }

    private void createPulsingBloodEffect() {
    }

    private void animateBloodAura() {
        Location center = caster.getLocation();
        if (terrainRing != null && currentTicks % 4 == 0) terrainRing.move(center);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onConfirmedWeaponHit(EntityDamageByEntityEvent event) {
        CombatSource source = event.getCombatSource();
        if (dealingCleaveDamage || event.getDamager() != caster || !(event.getEntity() instanceof LivingEntity target)
                || source == null || source.type() != CombatSource.Type.BASIC_ATTACK
                || event.getDamage() <= 0 || !Spellbook.canAttack(caster, target)) {
            return;
        }
        double healingPower = Math.min(80, Math.max(0, caster.getAttribute(Attribute.STAT_HEALINGPOWER).getValue()));
        double healingAmount = Math.min(remainingHitHealing, hitHealBase + 0.1 * healingPower);
        remainingHitHealing -= healingAmount;
        if (target instanceof Player || HealingContext.isPlayerCombat(caster)) {
            healingAmount *= playerHealingMultiplier;
        }
        if (healingAmount > 0) {
            caster.heal(healingAmount);
            playHealingEffect();
        }
        double cleavingDamage = event.getDamage() * cleavingDamageMultiplier;
        dealingCleaveDamage = true;
        try {
            for (LivingEntity entity : target.getLocation().getNearbyLivingEntities(cleavingRange)) {
                if (entity != target && entity != caster && Spellbook.canAttack(caster, entity)) {
                    CombatSourceContext.runSpell(this, () -> entity.damage(cleavingDamage, caster, PDamageType.PHYSICAL));
                    playCleavingEffect(entity);
                }
            }
        } finally {
            dealingCleaveDamage = false;
        }
    }

    private void playHealingEffect() {
        SkillSignatureVfx.accent(caster.getLocation().add(0, 1, 0), getClass(),
                "lead", 1.3f, 12, false);
    }

    private void playCleavingEffect(LivingEntity entity) {
        Location entityLoc = entity.getLocation().add(0, 1, 0);
        SkillSignatureVfx.accent(entityLoc, getClass(), "wake", 1.9f, 14, false);
        entity.getWorld().playSound(entityLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.4f, 1.3f);
    }

    @Override
    protected void addSpellPlaceholders() {
        spellAddedPlaceholders.add(Component.text(castHealBase, VALUE_COLOR));
        placeholderNames.add("castHealBase");
        spellAddedPlaceholders.add(Component.text(Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_MAGICAL, speedDurationMin, speedDurationMax, "speedDuration") / 20, VALUE_COLOR));
        placeholderNames.add("speedDuration");
    }
}
