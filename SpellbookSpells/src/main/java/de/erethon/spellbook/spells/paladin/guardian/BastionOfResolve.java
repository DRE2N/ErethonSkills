package de.erethon.spellbook.spells.paladin.guardian;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.combat.BossControl;
import de.erethon.spellbook.combat.CombatSourceContext;
import de.erethon.spellbook.spells.vfx.TerrainRing;
import de.erethon.spellbook.spells.vfx.SkillVfx;
import org.bukkit.Bukkit;
import org.bukkit.Location;
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

import java.util.List;

/** A visible, finite defense that can be collapsed into one shield slam. */
public class BastionOfResolve extends GuardianBaseSpell implements Listener {

    private final double radius = data.getDouble("range", 4);
    private final double baseAbsorb = data.getDouble("baseAbsorb", 18);
    private final double resistanceCoefficient = data.getDouble("resistanceCoefficient", 0.5);
    private Location center;
    private double remainingAbsorb;
    private double absorbed;
    private boolean collapsed;
    private TerrainRing sanctuaryBoundary;

    public BastionOfResolve(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = data.getInt("duration", 6) * 20;
    }

    @Override
    protected boolean onPrecast() {
        return super.onPrecast() && hasEnergy(caster, data);
    }

    @Override
    public boolean onCast() {
        if (!super.onCast()) return false;
        center = caster.getLocation().clone();
        double resistance = Math.max(0, caster.getAttribute(Attribute.RESISTANCE_PHYSICAL).getValue())
                + Math.max(0, caster.getAttribute(Attribute.RESISTANCE_MAGICAL).getValue());
        remainingAbsorb = Math.min(caster.getMaxHealth() * 0.4,
                baseAbsorb + Math.min(100, resistance) * resistanceCoefficient);
        if (SkillVfx.availableTo(caster)) {
            sanctuaryBoundary = new TerrainRing(center, radius,
                    "terrain_band_guardian", 48, 0.6f);
        }
        center.getWorld().playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 0.9f, 1.1f);
        Bukkit.getPluginManager().registerEvents(this, Spellbook.getInstance().getImplementer());
        return true;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void protect(EntityDamageEvent event) {
        if (collapsed || center == null || !(event.getEntity() instanceof LivingEntity ally)
                || Spellbook.canAttack(caster, ally) || ally.getWorld() != center.getWorld()
                || ally.getLocation().distanceSquared(center) > radius * radius
                || event.getDamage() <= 0 || remainingAbsorb <= 0) return;
        double blocked = Math.min(remainingAbsorb, event.getDamage());
        remainingAbsorb -= blocked;
        absorbed += blocked;
        event.setDamage(Math.max(0, event.getDamage() - blocked));
        ally.getWorld().spawnParticle(Particle.ENCHANTED_HIT, ally.getEyeLocation(),
                8, 0.35, 0.5, 0.35);
        ally.getWorld().playSound(ally.getLocation(), Sound.ITEM_SHIELD_BLOCK, 0.9f, 1.0f);
        if (remainingAbsorb <= 0) collapse();
    }

    @Override
    protected void onTick() {
        super.onTick();
        if (collapsed) return;
        if (currentTicks % 10 == 0) {
            center.getWorld().spawnParticle(Particle.END_ROD, center.clone().add(0, 0.2, 0),
                    4, radius * 0.6, 0.1, radius * 0.6, 0.01);
        }
        if (currentTicks >= 20 && caster instanceof Player player && player.isSneaking()
                && player.getWorld() == center.getWorld()
                && player.getLocation().distanceSquared(center) <= radius * radius) collapse();
    }

    private void collapse() {
        if (collapsed || center == null) return;
        collapsed = true;
        HandlerList.unregisterAll(this);
        if (sanctuaryBoundary != null) {
            sanctuaryBoundary.close();
            sanctuaryBoundary = null;
        }
        center.getWorld().playSound(center, Sound.ITEM_SHIELD_BREAK, 1.0f, 0.75f);
        center.getWorld().spawnParticle(Particle.SWEEP_ATTACK, center.clone().add(0, 0.8, 0),
                3, radius * 0.4, 0.2, radius * 0.4);
        for (LivingEntity target : center.getWorld().getNearbyLivingEntities(center, radius, 2.5, radius)) {
            if (!Spellbook.canAttack(caster, target)
                    || target.getLocation().distanceSquared(center) > radius * radius
                    || !caster.hasLineOfSight(target)) continue;
            double damage = Math.min(caster.getMaxHealth() * 0.3,
                    Spellbook.getVariedAttributeBasedDamage(data, caster, target, false,
                            Attribute.ADVANTAGE_MAGICAL, "collapseDamage") + absorbed * 0.35);
            double before = target.getHealth() + target.getAbsorptionAmount();
            CombatSourceContext.runSpell(this, () -> target.damage(damage, caster, PDamageType.MAGIC));
            if (target.getHealth() + target.getAbsorptionAmount() < before) {
                BossControl.apply(caster, target, 40);
            }
        }
    }

    @Override
    protected void onTickFinish() {
        if (!collapsed) collapse();
        super.onTickFinish();
    }

    @Override
    protected void cleanup() {
        HandlerList.unregisterAll(this);
        if (sanctuaryBoundary != null) sanctuaryBoundary.close();
        super.cleanup();
    }
}
