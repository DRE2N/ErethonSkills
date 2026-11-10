package de.erethon.spellbook.spells.assassin.shadow;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.combat.BossControl;
import de.erethon.spellbook.combat.CombatSourceContext;
import de.erethon.spellbook.spells.vfx.SkillSignatureVfx;
import de.erethon.spellbook.spells.vfx.SkillVfx;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Fyreum
 */
public class DaggerThrow extends ShadowBaseSpell implements Listener {

    // A blockable projectile applies an owner-specific, visible mark on a confirmed hit.

    private final float speed = (float) data.getDouble("speed", 2.0);
    private final int divergence = data.getInt("divergence", 1);

    private Arrow arrow = null;
    private String markToken;
    private ItemDisplay daggerDisplay = null;
    private ItemDisplay markSigil;

    public DaggerThrow(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        tickInterval = 1;
        keepAliveTicks = duration * 20;
    }

    @Override
    public boolean onCast() {
        if (!super.onCast()) return false;
        Bukkit.getServer().getPluginManager().registerEvents(this, Spellbook.getInstance().getImplementer());
        Vector direction = caster.getEyeLocation().getDirection();
        // Slightly in front of the caster to avoid hitting self
        Location spawnLocation = caster.getEyeLocation().add(direction.clone().multiply(0.5));
        arrow = caster.getWorld().spawnArrow(spawnLocation, direction, speed, divergence);
        arrow.setShooter(caster);
        arrow.setInvisible(true);
        arrow.setDamage(0);
        daggerDisplay = trackVfx(SkillVfx.projectile(spawnLocation, direction,
                "sig_assassin_shadow_daggerthrow_lead_f4", 1.5f));

        // Enhanced sound effect for throwing
        caster.getWorld().playSound(spawnLocation, Sound.ENTITY_ARROW_SHOOT, SoundCategory.RECORDS, 0.8f, 0.7f);
        caster.getWorld().playSound(spawnLocation, Sound.BLOCK_ANVIL_BREAK, SoundCategory.RECORDS, 0.3f, 2.0f);

        return true;
    }

    @EventHandler
    private void onHit(ProjectileHitEvent event) {
        if (arrow != null && event.getEntity() == arrow) {
           if (event.getHitEntity() instanceof LivingEntity entity) {
                if (entity == caster || !Spellbook.canAttack(caster, entity)) {
                    arrow.remove();
                    daggerDisplay.remove();
                    return;
                }
                double damage = Spellbook.getVariedAttributeBasedDamage(data, caster, entity,
                        true, Attribute.ADVANTAGE_PHYSICAL);
                double before = entity.getHealth() + entity.getAbsorptionAmount();
                CombatSourceContext.runSpell(this, () -> entity.damage(damage, caster, PDamageType.PHYSICAL));
                if (entity.getHealth() + entity.getAbsorptionAmount() >= before) {
                    arrow.remove();
                    daggerDisplay.remove();
                    return;
                }
                target = entity;
                markToken = ShadowMark.apply(caster, entity);
                BossControl.apply(caster, entity, 10);
                triggerTraits(target);

                entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, SoundCategory.RECORDS, 1.0f, 1.5f);
                entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_SOUL_SAND_BREAK, SoundCategory.RECORDS, 0.8f, 0.8f);

                entity.getWorld().spawnParticle(Particle.DUST, entity.getLocation().add(0, 1, 0), 15, 0.8, 0.8, 0.8, 0.1, new Particle.DustOptions(Color.BLACK, 1.2f));
                entity.getWorld().spawnParticle(Particle.SMOKE, entity.getLocation().add(0, 1, 0), 8, 0.5, 0.5, 0.5, 0.05);
                SkillSignatureVfx.accent(entity.getLocation().add(0, 1.2, 0), getClass(),
                        "main", 2.4f, 19, false);

                arrow.remove();
                daggerDisplay.remove();

                createMarkDisplays(entity);
            } else {
                Location hitLoc = arrow.getLocation();
                hitLoc.getWorld().spawnParticle(Particle.DUST, hitLoc, 8, 0.3, 0.3, 0.3, 0.1, new Particle.DustOptions(Color.BLACK, 1.0f));
                hitLoc.getWorld().playSound(hitLoc, Sound.BLOCK_STONE_HIT, SoundCategory.RECORDS, 0.6f, 1.2f);
                arrow.remove();
                daggerDisplay.remove();
            }
        }
    }

    @Override
    protected void onTick() {
        if (arrow != null && !arrow.isDead()) {
            Location arrowLoc = arrow.getLocation();
            if (daggerDisplay != null && daggerDisplay.isValid()) {
                daggerDisplay.teleport(arrowLoc);
                SkillVfx.orientProjectile(daggerDisplay, arrow.getVelocity());
            }
            if (currentTicks % 3 == 0) SkillSignatureVfx.accent(arrowLoc, getClass(),
                    "wake", 0.9f, 9, false, arrow.getVelocity());
            arrowLoc.getWorld().spawnParticle(Particle.DUST, arrowLoc, 2, 0.1, 0.1, 0.1, 0, new Particle.DustOptions(Color.fromRGB(64, 64, 64), 0.8f));
            arrowLoc.getWorld().spawnParticle(Particle.WHITE_ASH, arrowLoc, 1, 0.05, 0.05, 0.05, 0.01);
        }

        if (target == null) {
            return;
        }

        // Backstab consumes the mark. Remove its client-side ground effect at
        // the same time instead of leaving the fake blocks behind.
        if (!ShadowMark.isCurrent(target, markToken)) {
            removeMarkingAoE();
            target = null;
            return;
        }

        if (markSigil != null && markSigil.isValid())
            markSigil.teleport(target.getEyeLocation().add(0, 0.38, 0));

        target.getWorld().spawnParticle(Particle.SMOKE, target.getLocation().add(0, 2, 0), 2, 0.3, 0.1, 0.3, 0.01);
    }

    @Override
    protected void onTickFinish() {
        super.onTickFinish();
    }

    @Override
    protected void cleanup() {
        HandlerList.unregisterAll(this);
        if (arrow != null && !arrow.isDead()) {
            arrow.remove();
        }
        if (target != null) {
            ShadowMark.clearIfCurrent(caster, target, markToken);
            target = null;
        }
        removeMarkingAoE();
        super.cleanup();
    }

    private void removeMarkingAoE() {
        if (markSigil != null && markSigil.isValid()) markSigil.remove();
        markSigil = null;
    }

    private void createMarkDisplays(LivingEntity marked) {
        markSigil = trackVfx(SkillVfx.billboard(marked.getEyeLocation().add(0, 0.38, 0),
                "sig_assassin_shadow_daggerthrow_wake_f4", 0.95f));
    }

}
