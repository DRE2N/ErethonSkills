package de.erethon.spellbook.spells.assassin.shadow;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.combat.HealingContext;
import de.erethon.spellbook.spells.vfx.SkillSignatureVfx;
import de.erethon.spellbook.spells.vfx.TerrainRing;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Visible withdrawal with timed effects that expire even if spell cleanup is interrupted. */
public class ShadowCloak extends ShadowBaseSpell implements Listener {

    private static final Map<UUID, ShadowCloak> ACTIVE_CLOAKS = new HashMap<>();

    private final double baseHealing = data.getDouble("baseHealing", 18);
    private final double healingPowerCoefficient = data.getDouble("healingPowerCoefficient", 0.35);
    private final double playerCombatHealingMultiplier = data.getDouble("playerCombatHealingMultiplier", 0.6);
    private double pulseHealing;
    private double laterPulseMultiplier = 1.0;
    private int pulses;
    private PotionEffect previousInvisible;
    private PotionEffect previousGlowing;
    private boolean active;
    private TerrainRing shadowRing;

    public ShadowCloak(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = 60;
        tickInterval = 1;
    }

    @Override
    protected boolean onPrecast() {
        ShadowCloak existing = ACTIVE_CLOAKS.get(caster.getUniqueId());
        return (existing == null || !existing.active) && super.onPrecast();
    }

    @Override
    public boolean onCast() {
        if (!super.onCast()) return false;
        active = true;
        ACTIVE_CLOAKS.put(caster.getUniqueId(), this);
        previousInvisible = caster.getPotionEffect(PotionEffectType.INVISIBILITY);
        previousGlowing = caster.getPotionEffect(PotionEffectType.GLOWING);
        // Repair flags left behind by the old Cloak implementation on an existing character.
        if (previousInvisible == null && caster.isInvisible()) caster.setInvisible(false);
        if (previousGlowing == null && caster.isGlowing()) caster.setGlowing(false);
        // Potion effects expire on their own if the spell instance stops ticking unexpectedly.
        caster.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, keepAliveTicks, 0, true, false, false));
        caster.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, keepAliveTicks, 0, true, false, false));
        Bukkit.getScheduler().runTaskLater(Spellbook.getInstance().getImplementer(), this::endCloak,
                keepAliveTicks);
        double healingPower = caster.getAttribute(Attribute.STAT_HEALINGPOWER) == null ? 0
                : Math.max(0, Math.min(80, caster.getAttribute(Attribute.STAT_HEALINGPOWER).getValue()));
        double totalHealing = baseHealing + healingPower * healingPowerCoefficient;
        if (HealingContext.isPlayerCombat(caster)) {
            totalHealing *= playerCombatHealingMultiplier;
        }
        pulseHealing = Math.min(caster.getMaxHealth() * 0.3, totalHealing) / 4.0;
        laterPulseMultiplier = 1.0;
        pulses = 1;
        caster.heal(pulseHealing);
        Bukkit.getPluginManager().registerEvents(this, Spellbook.getInstance().getImplementer());
        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 0.8f, 0.85f);
        shadowRing = new TerrainRing(caster.getLocation(), 1.25,
                "terrain_band_shadow", 24, 0.28f);
        SkillSignatureVfx.accent(caster.getLocation(), getClass(), "main", 3.2f, 23, true);
        return true;
    }

    @Override
    protected void onTick() {
        super.onTick();
        if (shadowRing != null && currentTicks % 4 == 0) shadowRing.move(caster.getLocation());
        if (currentTicks % 4 == 0) {
            caster.getWorld().spawnParticle(Particle.DUST, caster.getLocation().add(0, 1, 0),
                    3, 0.25, 0.6, 0.25, 0,
                    new Particle.DustOptions(Color.fromRGB(126, 135, 143), 0.85f));
            caster.getWorld().spawnParticle(Particle.SMOKE, caster.getLocation().add(0, 0.3, 0),
                    2, 0.25, 0.15, 0.25, 0.01);
        }
        if (currentTicks % 15 != 0 || pulses >= 4) return;
        pulses++;
        caster.heal(pulseHealing * laterPulseMultiplier);
        SkillSignatureVfx.accent(caster.getLocation(), getClass(), "wake", 2.2f, 17, true);
        caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_SOUL_SAND_STEP, 0.3f, 1.5f);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() == caster && event.getFinalDamage() > 0) {
            laterPulseMultiplier = Math.max(0.35, laterPulseMultiplier * 0.6);
        }
    }

    @Override
    public double onAttack(LivingEntity target, double damage, PDamageType type) {
        currentTicks = keepAliveTicks;
        endCloak();
        return super.onAttack(target, damage, type);
    }

    private void endCloak() {
        if (!active) return;
        active = false;
        ACTIVE_CLOAKS.remove(caster.getUniqueId(), this);
        caster.removePotionEffect(PotionEffectType.INVISIBILITY);
        caster.removePotionEffect(PotionEffectType.GLOWING);
        if (previousInvisible != null && previousInvisible.getDuration() > currentTicks) {
            caster.addPotionEffect(previousInvisible.withDuration(previousInvisible.getDuration() - currentTicks));
        }
        if (previousGlowing != null && previousGlowing.getDuration() > currentTicks) {
            caster.addPotionEffect(previousGlowing.withDuration(previousGlowing.getDuration() - currentTicks));
        }
    }

    @Override
    protected void cleanup() {
        if (shadowRing != null) shadowRing.close();
        HandlerList.unregisterAll(this);
        endCloak();
        super.cleanup();
    }
}
