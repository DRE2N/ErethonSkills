package de.erethon.spellbook.spells.assassin.shadow;

import de.erethon.papyrus.PDamageType;
import de.erethon.papyrus.combat.CombatSource;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.combat.BossControl;
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
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.Vector;

import java.util.List;

/** A shown short lunge followed by a manual weapon hit. Rear positioning earns the payoff. */
public class Backstab extends ShadowBaseSpell implements Listener {

    private final int range = data.getInt("range", 8);
    private final double baseFollowupMultiplier = data.getDouble("baseFollowupMultiplier", 1.15);
    private final double flankBonusMin = data.getDouble("flankBonusMin", 1.6);
    private final double flankBonusMax = data.getDouble("flankBonusMax", 2.2);
    private final double flankBonusMagicalCoefficient = data.getDouble("flankBonusMagicalCoefficient", 0.0075);
    private Location landing;
    private boolean arrived;
    private boolean spent;
    private TerrainRing terrainRing;

    public Backstab(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = 48;
        tickInterval = 1;
    }

    @Override
    protected boolean onPrecast() {
        lookForTarget(range);
        if (target == null || !caster.hasLineOfSight(target) || !Spellbook.canAttack(caster, target)) {
            SpellbookFeedback.send(caster, "<red>No visible target in range.");
            return false;
        }
        Vector approach = target.getLocation().toVector().subtract(caster.getLocation().toVector());
        approach.setY(0);
        if (approach.lengthSquared() < 0.01) return false;
        landing = target.getLocation().clone().subtract(approach.normalize().multiply(1.7));
        landing.setYaw(caster.getLocation().getYaw());
        landing.setPitch(caster.getLocation().getPitch());
        if (!isLandingOpen()) {
            SpellbookFeedback.send(caster, "<red>Landing point is blocked.");
            return false;
        }
        return super.onPrecast();
    }

    private boolean isLandingOpen() {
        return landing.getBlock().isPassable()
                && landing.clone().add(0, 1, 0).getBlock().isPassable();
    }

    @Override
    public boolean onCast() {
        if (!super.onCast()) return false;
        arrived = false;
        spent = false;
        terrainRing = new TerrainRing(landing, 1.25,
                "terrain_band_shadow", 24, 0.3f);
        SkillSignatureVfx.accent(landing, getClass(), "lead", 2.2f, 9, true);
        landing.getWorld().playSound(landing, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.8f, 1.5f);
        Bukkit.getPluginManager().registerEvents(this, Spellbook.getInstance().getImplementer());
        return true;
    }

    @Override
    protected void onTick() {
        super.onTick();
        if (arrived || currentTicks < 8) return;
        if (!caster.isValid() || target == null || !target.isValid() || target.isDead()
                || target.getWorld() != caster.getWorld() || !isLandingOpen()
                || !caster.hasLineOfSight(target)) {
            currentTicks = keepAliveTicks;
            return;
        }
        Location start = caster.getLocation().clone();
        if (!caster.teleport(landing, PlayerTeleportEvent.TeleportCause.PLUGIN)) {
            currentTicks = keepAliveTicks;
            return;
        }
        arrived = true;
        SkillSignatureVfx.accent(start.add(0, 1, 0), getClass(), "wake", 1.7f, 14, false);
        SkillSignatureVfx.accent(landing.clone().add(0, 1, 0), getClass(), "main", 2.6f, 15, false);
        landing.getWorld().playSound(landing, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.25f);
    }

    private boolean isWeaponHit(EntityDamageByEntityEvent event) {
        CombatSource source = event.getCombatSource();
        return arrived && !spent && event.getDamager() == caster && event.getEntity() == target
                && event.getDamageType() == PDamageType.PHYSICAL && source != null
                && source.type() == CombatSource.Type.BASIC_ATTACK;
    }

    private boolean rearAngle() {
        Vector facing = target.getLocation().getDirection().setY(0);
        Vector toCaster = caster.getLocation().toVector().subtract(target.getLocation().toVector()).setY(0);
        return facing.lengthSquared() > 0.01 && toCaster.lengthSquared() > 0.01
                && facing.normalize().dot(toCaster.normalize()) < -0.45;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void empowerFollowup(EntityDamageByEntityEvent event) {
        if (!isWeaponHit(event) || event.getDamage() <= 0) return;
        boolean marked = target.getTags().contains(ShadowMark.tag(caster));
        boolean boss = BossControl.apply(caster, target, 0);
        double magicalAdvantage = caster.getAttribute(Attribute.ADVANTAGE_MAGICAL) == null ? 0
                : Math.max(0, caster.getAttribute(Attribute.ADVANTAGE_MAGICAL).getValue());
        double multiplier = marked && (rearAngle() || boss)
                ? Math.min(flankBonusMax, flankBonusMin + magicalAdvantage * flankBonusMagicalCoefficient)
                : baseFollowupMultiplier;
        event.setDamage(event.getDamage() * multiplier);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void confirmFollowup(EntityDamageByEntityEvent event) {
        if (!isWeaponHit(event) || event.getFinalDamage() <= 0) return;
        spent = true;
        boolean marked = ShadowMark.consume(caster, target);
        boolean earned = marked && (rearAngle() || BossControl.apply(caster, target, 0));
        BossControl.apply(caster, target, earned ? 50 : 15);
        target.getWorld().playSound(target.getLocation(), earned
                ? Sound.ENTITY_PLAYER_ATTACK_CRIT : Sound.ENTITY_PLAYER_ATTACK_STRONG,
                earned ? 1.1f : 0.6f, earned ? 0.75f : 1.1f);
        SkillSignatureVfx.accent(target.getEyeLocation(), getClass(),
                earned ? "main" : "wake", earned ? 3.2f : 1.7f, 19, false);
    }

    @Override
    protected void cleanup() {
        if (terrainRing != null) terrainRing.close();
        HandlerList.unregisterAll(this);
        super.cleanup();
    }
}
