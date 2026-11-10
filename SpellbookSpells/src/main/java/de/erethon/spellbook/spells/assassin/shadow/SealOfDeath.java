package de.erethon.spellbook.spells.assassin.shadow;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.combat.BossControl;
import de.erethon.spellbook.combat.CombatSourceContext;
import de.erethon.spellbook.spells.vfx.SkillSignatureVfx;
import de.erethon.spellbook.spells.vfx.TerrainRing;
import de.erethon.spellbook.utils.SpellbookFeedback;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.List;

/** A visible armed seal that needs a real followup hit and an exposed sightline. */
public class SealOfDeath extends ShadowBaseSpell implements Listener {

    private final int range = data.getInt("range", 12);
    private final int armTicks = data.getInt("armTicks", 12);
    private final double splashMultiplier = data.getDouble("splashMultiplier", 0.5);
    private boolean armed;
    private boolean detonated;
    private TerrainRing terrainRing;

    public SealOfDeath(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = 80;
        tickInterval = 1;
    }

    @Override
    protected boolean onPrecast() {
        lookForTarget(range);
        if (target == null || !target.getTags().contains(ShadowMark.tag(caster))
                || !caster.hasLineOfSight(target)) {
            SpellbookFeedback.send(caster, "<red>Mark a visible target first.");
            return false;
        }
        return super.onPrecast();
    }

    @Override
    public boolean onCast() {
        if (!super.onCast()) return false;
        target.getTags().add("sealofdeath.active");
        armed = false;
        detonated = false;
        terrainRing = new TerrainRing(target.getLocation(), 2.2,
                "terrain_band_shadow", 36, 0.34f);
        SkillSignatureVfx.accent(target.getLocation(), getClass(), "lead", 4.0f, 20, true);
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 0.6f);
        target.getWorld().spawnParticle(Particle.SMOKE, target.getEyeLocation(),
                12, 0.4, 0.5, 0.4, 0.03);
        Bukkit.getPluginManager().registerEvents(this, Spellbook.getInstance().getImplementer());
        return true;
    }

    @Override
    protected void onTick() {
        super.onTick();
        if (detonated || target == null || !target.isValid() || target.isDead()
                || !target.getTags().contains(ShadowMark.tag(caster))) {
            currentTicks = keepAliveTicks;
            return;
        }
        if (!armed && currentTicks >= armTicks) {
            armed = true;
            SkillSignatureVfx.accent(target.getLocation().add(0, 1.1, 0), getClass(),
                    "main", 2.5f, 15, false);
            target.getWorld().playSound(target.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE,
                    1.0f, 1.35f);
        }
        if (currentTicks % 5 == 0) {
            terrainRing.move(target.getLocation());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFollowupHit(EntityDamageByEntityEvent event) {
        if (!armed || detonated || event.getEntity() != target || event.getDamager() != caster
                || event.getFinalDamage() <= 0 || target.getWorld() != caster.getWorld()
                || target.getLocation().distanceSquared(caster.getLocation()) > range * range
                || !caster.hasLineOfSight(target)) return;
        detonated = true;
        Bukkit.getScheduler().runTask(Spellbook.getInstance().getImplementer(), () -> {
            if (target.isValid() && !target.isDead() && Spellbook.canAttack(caster, target)
                    && caster.hasLineOfSight(target)) detonate();
            currentTicks = keepAliveTicks;
        });
    }

    private void detonate() {
        double damage = Spellbook.getVariedAttributeBasedDamage(data, caster, target,
                true, Attribute.ADVANTAGE_MAGICAL);
        double before = target.getHealth() + target.getAbsorptionAmount();
        CombatSourceContext.runSpell(this, () -> target.damage(damage, caster, PDamageType.MAGIC));
        if (target.getHealth() + target.getAbsorptionAmount() >= before) return;
        BossControl.apply(caster, target, 30);
        Location center = target.getLocation().add(0, 1, 0);
        center.getWorld().playSound(center, Sound.ENTITY_WITHER_BREAK_BLOCK, 1.0f, 1.25f);
        SkillSignatureVfx.accent(center, getClass(), "main", 5.0f, 25, false);
        SkillSignatureVfx.accent(target.getLocation(), getClass(), "wake", 6.0f, 25, true);
        int splashed = 0;
        for (LivingEntity other : center.getNearbyLivingEntities(3)) {
            if (other == caster || other == target || other instanceof Player
                    || !Spellbook.canAttack(caster, other) || !caster.hasLineOfSight(other)) continue;
            double splash = Spellbook.getVariedAttributeBasedDamage(data, caster, other,
                    true, Attribute.ADVANTAGE_MAGICAL) * splashMultiplier;
            CombatSourceContext.runSpell(this, () -> other.damage(splash, caster, PDamageType.MAGIC));
            if (++splashed >= 3) break;
        }
    }

    @Override
    protected void cleanup() {
        if (terrainRing != null) terrainRing.close();
        HandlerList.unregisterAll(this);
        if (target != null) target.getTags().remove("sealofdeath.active");
        super.cleanup();
    }
}
