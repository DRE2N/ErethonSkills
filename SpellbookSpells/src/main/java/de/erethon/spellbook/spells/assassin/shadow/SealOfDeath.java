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
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class SealOfDeath extends AssassinBaseSpell implements Listener {

    private final int range = data.getInt("range", 15);
    private final double bonusDamageMultiplier = data.getDouble("bonusDamageMultiplier", 1.4);
    private final double executionThreshold = data.getDouble("executionThreshold", 0.2);
    private final float blindnessRadius = (float) data.getDouble("blindnessRadius", 6.0);
    private final int blindnessDurationMin = data.getInt("blindnessDurationMin", 8) * 20;
    private final int blindnessDurationMax = data.getInt("blindnessDurationMax", 20) * 20;

    private final EffectData blindnessEffect = Spellbook.getEffectData("Blindness");

    private int visualTick = 15;
    private AoE sealAura = null;
    private AoE executionZone = null;
    private boolean sealActive = false;

    public SealOfDeath(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = duration * 20;
        tickInterval = 1;
    }

    @Override
    protected boolean onPrecast() {
        lookForTarget(range);
        if (target == null || !target.getTags().contains("assassin.daggerthrow.marked")) {
            caster.sendParsedActionBar("<red>Target is not marked!");
            return false;
        }
        return super.onPrecast();
    }

    @Override
    public boolean onCast() {
        Bukkit.getPluginManager().registerEvents(this, Spellbook.getInstance().getImplementer());

        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_WITHER_SPAWN, SoundCategory.RECORDS, 0.6f, 1.8f);
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_SOUL_SAND_BREAK, SoundCategory.RECORDS, 0.8f, 0.6f);
        target.getWorld().playSound(caster.getLocation(), Sound.ENTITY_PHANTOM_DEATH, SoundCategory.RECORDS, 0.7f, 0.8f);

        target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(0, 1, 0), 30, 1, 1, 1, 0.1, new Particle.DustOptions(Color.fromRGB(139, 0, 0), 1.5f));
        target.getWorld().spawnParticle(Particle.SMOKE, target.getLocation().add(0, 1, 0), 15, 0.8, 0.8, 0.8, 0.05);

        CircleEffect effect = new CircleEffect(Spellbook.getInstance().getEffectManager());
        effect.radius = 2.0f;
        effect.particle = Particle.WITCH;
        effect.particleCount = 15;
        effect.duration = 100;
        effect.setLocation(target.getLocation().clone().add(0, -0.5, 0));
        effect.start();

        sealAura = createCircularAoE(target.getLocation(), 2.5, 1, keepAliveTicks)
            .followEntity(target)
            .addBlockChange(Material.CRIMSON_NYLIUM)
            .sendBlockChanges();

        target.getTags().add("sealofdeath.active");
        sealActive = true;

        return super.onCast();
    }

    @Override
    protected void onTick() {
        super.onTick();
        if (!sealActive || target == null || target.isDead()) {
            return;
        }

        visualTick--;
        if (visualTick <= 0) {
            Location loc = target.getLocation().clone().add(0, 1.5, 0);
            loc.getWorld().spawnParticle(Particle.WITCH, loc, 8, 1.2, 0.3, 1.2, 0.1);
            loc.getWorld().spawnParticle(Particle.DUST, loc, 5, 0.8, 0.8, 0.8, 0, new Particle.DustOptions(Color.fromRGB(139, 0, 0), 1.0f));
            loc.getWorld().spawnParticle(Particle.WHITE_ASH, loc.clone().add(0, 1, 0), 3, 1, 0.5, 1, 0.02);
            visualTick = 15;
        }
    }

    @Override
    public double onAttack(LivingEntity attackedTarget, double damage, PDamageType type) {
        if (!sealActive || attackedTarget != this.target || !attackedTarget.getTags().contains("sealofdeath.active")) {
            return damage;
        }

        damage = damage * bonusDamageMultiplier;

        attackedTarget.getWorld().spawnParticle(Particle.CRIT, attackedTarget.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.2);
        attackedTarget.getWorld().spawnParticle(Particle.DUST, attackedTarget.getLocation().add(0, 1, 0), 8, 0.5, 0.5, 0.5, 0, new Particle.DustOptions(Color.fromRGB(139, 0, 0), 1.2f));
        attackedTarget.getWorld().playSound(attackedTarget.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.RECORDS, 1.0f, 0.7f);

        double health = attackedTarget.getHealth() - damage;
        double maxHealth = attackedTarget.getAttribute(Attribute.MAX_HEALTH).getValue();

        if (health <= maxHealth * executionThreshold) {
            executeTarget(attackedTarget);
        }

        return super.onAttack(attackedTarget, damage, type);
    }

    @EventHandler
    private void onDeath(PlayerDeathEvent event) {
        if (sealActive && event.getEntity().equals(target) && target.getTags().contains("sealofdeath.active")) {
            executeTarget(target);
        }
    }

    private void executeTarget(LivingEntity targetToExecute) {
        if (!sealActive) return;

        Location deathLoc = targetToExecute.getLocation().clone();
        int blindnessDuration = (int) Spellbook.getRangedValue(data, caster, targetToExecute, Attribute.ADVANTAGE_MAGICAL, blindnessDurationMin, blindnessDurationMax, "blindnessDuration");

        targetToExecute.getWorld().playSound(deathLoc, Sound.ENTITY_WITHER_DEATH, SoundCategory.RECORDS, 0.8f, 1.2f);
        targetToExecute.getWorld().playSound(deathLoc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.RECORDS, 0.6f, 0.8f);
        targetToExecute.getWorld().playSound(deathLoc, Sound.BLOCK_SOUL_SAND_BREAK, SoundCategory.RECORDS, 1.0f, 0.5f);

        targetToExecute.getWorld().spawnParticle(Particle.EXPLOSION, deathLoc.add(0, 1, 0), 3, 0.3, 0.3, 0.3, 0.1);
        targetToExecute.getWorld().spawnParticle(Particle.DUST, deathLoc, 40, 2, 2, 2, 0.2, new Particle.DustOptions(Color.BLACK, 2.0f));
        targetToExecute.getWorld().spawnParticle(Particle.SMOKE, deathLoc, 25, 3, 2, 3, 0.15);
        targetToExecute.getWorld().spawnParticle(Particle.SOUL, deathLoc, 15, 1.5, 1, 1.5, 0.1);

        targetToExecute.damage(1000000, caster, PDamageType.PHYSICAL);

        executionZone = createCircularAoE(deathLoc, blindnessRadius, 2, blindnessDuration)
            .addBlockChange(Material.SOUL_SOIL)
            .sendBlockChanges();

        for (LivingEntity entity : deathLoc.getNearbyLivingEntities(blindnessRadius)) {
            if (entity != caster && Spellbook.canAttack(caster, entity)) {
                entity.addEffect(caster, blindnessEffect, blindnessDuration, 2);
            }
        }

        sealActive = false;
        currentTicks = keepAliveTicks;
        onTickFinish();
    }

    @Override
    protected void onTickFinish() {
        super.onTickFinish();
        sealActive = false;
        if (target != null) {
            target.getTags().remove("sealofdeath.active");
        }
    }

    @Override
    protected void cleanup() {
        super.cleanup();
        HandlerList.unregisterAll(this);
        sealActive = false;
        if (target != null) {
            target.getTags().remove("sealofdeath.active");
        }
        if (sealAura != null) {
            sealAura.revertBlockChanges();
        }
        if (executionZone != null) {
            executionZone.revertBlockChanges();
        }
    }

    @Override
    protected void addSpellPlaceholders() {
        spellAddedPlaceholders.add(Component.text(Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_MAGICAL, blindnessDurationMin, blindnessDurationMax, "blindnessDuration"), VALUE_COLOR));
        placeholderNames.add("blindness");
    }
}
