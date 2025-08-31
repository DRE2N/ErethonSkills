package de.erethon.spellbook.spells.assassin.shadow;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.aoe.AoE;
import de.erethon.spellbook.spells.assassin.AssassinBaseSpell;
import de.slikey.effectlib.effect.CircleEffect;
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
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.util.Vector;

/**
 * @author Fyreum
 */
public class DaggerThrow extends AssassinBaseSpell implements Listener {

    // Throws a dagger at the target, dealing physical damage and applying a slow effect. Marks the target for a short duration.
    // Slow scales with advantage_magical.

    private final float speed = (float) data.getDouble("speed", 2.0);
    private final int divergence = data.getInt("divergence", 1);
    private final int effectDurationMin = data.getInt("slowDurationMin", 3) * 20;
    private final int effectDurationMax = data.getInt("slowDurationMax", 6) * 20;
    private final int effectStacksMin = data.getInt("slowStacksMin", 5);
    private final int effectStacksMax = data.getInt("slowStacksMax", 8);

    private final EffectData slowEffect = Spellbook.getEffectData("Slow");

    private Arrow arrow = null;
    private AoE markingAoE = null;

    public DaggerThrow(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        tickInterval = 1;
        keepAliveTicks = duration * 20;
        Bukkit.getServer().getPluginManager().registerEvents(this, Spellbook.getInstance().getImplementer());
    }

    @Override
    public boolean onCast() {
        Vector direction = caster.getEyeLocation().getDirection();
        // Slightly in front of the caster to avoid hitting self
        Location spawnLocation = caster.getEyeLocation().add(direction.clone().multiply(0.5));
        arrow = caster.getWorld().spawnArrow(spawnLocation, direction, speed, divergence);
        arrow.setInvisible(true);
        arrow.setDamage(1);

        // Enhanced sound effect for throwing
        caster.getWorld().playSound(spawnLocation, Sound.ENTITY_ARROW_SHOOT, SoundCategory.RECORDS, 0.8f, 0.7f);
        caster.getWorld().playSound(spawnLocation, Sound.BLOCK_ANVIL_BREAK, SoundCategory.RECORDS, 0.3f, 2.0f);

        return super.onCast();
    }

    @EventHandler
    private void onHit(ProjectileHitEvent event) {
        if (arrow != null && event.getEntity() == arrow) {
           if (event.getHitEntity() instanceof LivingEntity entity) {
                if (entity == caster || !Spellbook.canAttack(caster, entity)) {
                    return;
                }
                target = entity;
                entity.damage(Spellbook.getVariedAttributeBasedDamage(data, caster, entity, false, Attribute.ADVANTAGE_PHYSICAL), caster, PDamageType.PHYSICAL);
                triggerTraits(target);

                double slowDuration = Spellbook.getRangedValue(data, caster, target, Attribute.ADVANTAGE_MAGICAL, effectDurationMin, effectDurationMax, "slowDuration");
                double slowStacks = Spellbook.getRangedValue(data, caster, target, Attribute.ADVANTAGE_MAGICAL, effectStacksMin, effectStacksMax, "slowStacks");
                entity.addEffect(caster, slowEffect, (int) slowDuration, (int) slowStacks);

                entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, SoundCategory.RECORDS, 1.0f, 1.5f);
                entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_SOUL_SAND_BREAK, SoundCategory.RECORDS, 0.8f, 0.8f);

                entity.getWorld().spawnParticle(Particle.DUST, entity.getLocation().add(0, 1, 0), 15, 0.8, 0.8, 0.8, 0.1, new Particle.DustOptions(Color.BLACK, 1.2f));
                entity.getWorld().spawnParticle(Particle.SMOKE, entity.getLocation().add(0, 1, 0), 8, 0.5, 0.5, 0.5, 0.05);

                arrow.remove();
                entity.getTags().add("assassin.daggerthrow.marked");

                markingAoE = createCircularAoE(entity.getLocation(), 1.5, 1, keepAliveTicks - currentTicks)
                    .followEntity(entity)
                    .addBlockChange(Material.GRAY_CONCRETE)
                    .sendBlockChanges();
            } else {
                Location hitLoc = arrow.getLocation();
                hitLoc.getWorld().spawnParticle(Particle.DUST, hitLoc, 8, 0.3, 0.3, 0.3, 0.1, new Particle.DustOptions(Color.BLACK, 1.0f));
                hitLoc.getWorld().playSound(hitLoc, Sound.BLOCK_STONE_HIT, SoundCategory.RECORDS, 0.6f, 1.2f);
                arrow.remove();
            }
        }
    }

    @Override
    protected void onTick() {
        if (arrow != null && !arrow.isDead()) {
            Location arrowLoc = arrow.getLocation();
            arrowLoc.getWorld().spawnParticle(Particle.DUST, arrowLoc, 2, 0.1, 0.1, 0.1, 0, new Particle.DustOptions(Color.fromRGB(64, 64, 64), 0.8f));
            arrowLoc.getWorld().spawnParticle(Particle.WHITE_ASH, arrowLoc, 1, 0.05, 0.05, 0.05, 0.01);
        }

        if (target == null) {
            return;
        }

        CircleEffect effect = new CircleEffect(Spellbook.getInstance().getEffectManager());
        effect.setLocation(target.getLocation().clone().add(0, 1.5, 0));
        effect.particle = org.bukkit.Particle.DUST;
        effect.particleCount = 6;
        effect.radius = 0.6f;
        effect.color = Color.fromRGB(128, 0, 0);
        effect.duration = 20;
        effect.start();

        target.getWorld().spawnParticle(Particle.SMOKE, target.getLocation().add(0, 2, 0), 2, 0.3, 0.1, 0.3, 0.01);
    }

    @Override
    protected void onTickFinish() {
        super.onTickFinish();
        if (target != null) {
            target.getTags().remove("assassin.daggerthrow.marked");
            target = null;
        }
        if (markingAoE != null) {
            markingAoE.revertBlockChanges();
        }
    }

    @Override
    protected void cleanup() {
        HandlerList.unregisterAll(this);
        if (markingAoE != null) {
            markingAoE.revertBlockChanges();
        }
    }

    @Override
    public void addSpellPlaceholders() {
        spellAddedPlaceholders.add(Component.text(Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_MAGICAL, effectDurationMin, effectDurationMax, "slowDuration") / 20, VALUE_COLOR));
        placeholderNames.add("slowDuration");
        spellAddedPlaceholders.add(Component.text(Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_MAGICAL, effectStacksMin, effectStacksMax, "slowStacks"), VALUE_COLOR));
        placeholderNames.add("slowStacks");
    }
}
