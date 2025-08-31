package de.erethon.spellbook.spells.assassin.cutthroat;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.aoe.AoE;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellEffect;
import de.erethon.spellbook.spells.assassin.AssassinBaseSpell;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.CircleEffect;
import de.slikey.effectlib.effect.LineEffect;
import de.slikey.effectlib.effect.SphereEffect;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class CoupDeGrace extends AssassinBaseSpell {

    // Ultimate ability: Jumps to a target, slashing their throat, dealing massive damage.
    // Damage increased by 10% for each stack of bleeding on the target. Scales with advantage_magical.
    // If the target has less than 25% health, they instantly die.

    private final double bonusPerBleedingMin = data.getDouble("bleedingBonusMin", 0.1);
    private final double bonusPerBleedingMax = data.getDouble("bleedingBonusMax", 0.5);
    private final double executionThreshold = data.getDouble("executionThreshold", 0.25);
    private final double executionAuraRadius = data.getDouble("executionAuraRadius", 4.0);

    private final EffectData bleedingEffectData = Spellbook.getEffectData("Bleeding");

    private boolean hasJumped = false;
    private boolean hasExecuted = false;

    public CoupDeGrace(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = 100;
    }

    @Override
    protected boolean onPrecast() {
        return super.onPrecast() && lookForTarget();
    }

    @Override
    public boolean onCast() {
        if (target == null) {
            return false;
        }

        playAssassinationPrelude();
        return super.onCast();
    }

    private void playAssassinationPrelude() {
        Location casterLoc = caster.getLocation().add(0, 1, 0);
        Location targetLoc = target.getLocation().add(0, 1, 0);

        caster.getWorld().spawnParticle(Particle.SMOKE, casterLoc, 15, 0.5, 0.5, 0.5, 0.05);
        caster.getWorld().spawnParticle(Particle.ENCHANTED_HIT, targetLoc, 10, 0.3, 0.3, 0.3, 0.1);

        caster.getWorld().playSound(casterLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.4f, 1.8f);
        target.getWorld().playSound(targetLoc, Sound.ENTITY_WITHER_AMBIENT, 0.3f, 2.0f);

        EffectManager effectManager = Spellbook.getInstance().getEffectManager();
        if (effectManager != null) {
            LineEffect hunterMark = new LineEffect(effectManager);
            hunterMark.setLocation(casterLoc);
            hunterMark.setTarget(targetLoc);
            hunterMark.particle = Particle.DUST;
            hunterMark.particles = 20;
            hunterMark.duration = 20;
            hunterMark.start();

            CircleEffect targetMark = new CircleEffect(effectManager);
            targetMark.setLocation(targetLoc);
            targetMark.radius = 1.5f;
            targetMark.particle = Particle.DUST;
            targetMark.particles = 15;
            targetMark.duration = 100;
            targetMark.start();
        }
    }

    private void performAssassinationJump() {
        hasJumped = true;

        Vector toTarget = target.getLocation().subtract(caster.getLocation()).toVector();
        double distance = toTarget.length();
        toTarget.normalize();

        if (distance <= 2.0) {
            Vector leapVector = toTarget.multiply(0.8);
            leapVector.setY(0.3);
            caster.setVelocity(leapVector);
        } else if (distance <= 4.0) {
            Vector leapVector = toTarget.multiply(1.2);
            leapVector.setY(0.4);
            caster.setVelocity(leapVector);
        } else {
            Vector leapVector = toTarget.multiply(Math.min(distance * 0.6, 1.8));
            leapVector.setY(Math.max(0.5, distance * 0.08));
            caster.setVelocity(leapVector);
        }

        playEnhancedLeapEffects();

        new BukkitRunnable() {
            int attempts = 0;

            @Override
            public void run() {
                attempts++;
                double currentDistance = target.getLocation().distance(caster.getLocation());

                if (currentDistance <= 2.5 || attempts >= 30) {
                    this.cancel();
                    if (!hasExecuted) {
                        executeAssassination();
                    }
                } else if (attempts > 8 && caster.isOnGround()) {
                    Vector additionalBoost = target.getLocation().subtract(caster.getLocation()).toVector().normalize();
                    additionalBoost.multiply(0.6).setY(0.2);
                    caster.setVelocity(additionalBoost);
                }
            }
        }.runTaskTimer(Spellbook.getInstance().getImplementer(), 2L, 1L);
    }

    private void playEnhancedLeapEffects() {
        Location casterLoc = caster.getLocation().add(0, 1, 0);
        Location targetLoc = target.getLocation().add(0, 1, 0);

        caster.getWorld().spawnParticle(Particle.EXPLOSION, casterLoc, 2, 0.3, 0.3, 0.3, 0);
        caster.getWorld().spawnParticle(Particle.CLOUD, casterLoc, 15, 0.8, 0.2, 0.8, 0.2);
        caster.getWorld().spawnParticle(Particle.DUST, casterLoc, 20, 0.5, 0.5, 0.5, 0,
            new Particle.DustOptions(org.bukkit.Color.BLACK, 2.0f));

        caster.getWorld().playSound(casterLoc, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 1.2f);
        caster.getWorld().playSound(casterLoc, Sound.ENTITY_WITHER_SHOOT, 0.6f, 1.8f);
        target.getWorld().playSound(targetLoc, Sound.BLOCK_GLASS_BREAK, 1.2f, 0.5f);

        createLeapTrail();
    }

    private void createLeapTrail() {
        new BukkitRunnable() {
            int ticks = 0;
            Location lastPos = caster.getLocation().clone();

            @Override
            public void run() {
                if (ticks >= 40 || hasExecuted || caster.isDead()) { // 2 second max
                    this.cancel();
                    return;
                }

                Location currentPos = caster.getLocation().add(0, 1, 0);

                currentPos.getWorld().spawnParticle(Particle.DUST, currentPos, 3, 0.2, 0.2, 0.2, 0,
                    new Particle.DustOptions(org.bukkit.Color.MAROON, 1.5f));
                currentPos.getWorld().spawnParticle(Particle.SMOKE, currentPos, 2, 0.1, 0.1, 0.1, 0.02);

                if (lastPos.distance(currentPos) > 0.5) {
                    Vector direction = currentPos.toVector().subtract(lastPos.toVector()).normalize();
                    double distance = lastPos.distance(currentPos);

                    for (double i = 0; i < distance; i += 0.3) {
                        Location trailPos = lastPos.clone().add(direction.clone().multiply(i));
                        trailPos.getWorld().spawnParticle(Particle.DUST, trailPos, 1, 0.1, 0.1, 0.1, 0,
                            new Particle.DustOptions(org.bukkit.Color.BLACK, 1.0f));
                    }
                    lastPos = currentPos.clone();
                }

                ticks++;
            }
        }.runTaskTimer(Spellbook.getInstance().getImplementer(), 1L, 1L);
    }

    @Override
    protected void onTick() {
        if (target == null || target.isDead()) {
            currentTicks = keepAliveTicks;
            onTickFinish();
            return;
        }

        double distance = target.getLocation().distance(caster.getLocation());

        if (!hasJumped && !hasExecuted) {
            if (distance > 3) {
                performAssassinationJump();
            } else {
                hasJumped = true;
                executeAssassination();
            }
        }
    }

    private void executeAssassination() {
        hasExecuted = true;

        double bonusFromBleeding = 0;
        double scaledBonus = Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_MAGICAL, bonusPerBleedingMin, bonusPerBleedingMax, "bleedingBonus");
        int bleedingStacks = 0;

        for (SpellEffect effect : target.getEffects()) {
            if (effect.data == bleedingEffectData) {
                bleedingStacks += effect.getStacks();
                bonusFromBleeding += scaledBonus * effect.getStacks();
            }
        }

        double baseDamage = Spellbook.getVariedAttributeBasedDamage(data, caster, target, true, Attribute.ADVANTAGE_PHYSICAL);
        double totalDamage = baseDamage + bonusFromBleeding;

        playExecutionStrike(bleedingStacks);

        target.damage(totalDamage, caster);

        double healthPercent = target.getHealth() / target.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();

        if (healthPercent < executionThreshold) {
            performExecution();
        } else {
            performRegularHit();
        }

        currentTicks = keepAliveTicks;
        onTickFinish();
    }

    private void playExecutionStrike(int bleedingStacks) {
        Location targetLoc = target.getLocation().add(0, 1, 0);

        target.getWorld().spawnParticle(Particle.CRIT, targetLoc, 15, 0.4, 0.4, 0.4, 0.3);
        target.getWorld().spawnParticle(Particle.ENCHANTED_HIT, targetLoc, 10, 0.3, 0.3, 0.3, 0.2);

        if (bleedingStacks > 0) {
            target.getWorld().spawnParticle(Particle.BLOCK, targetLoc, bleedingStacks * 3, 0.5, 0.5, 0.5, 0.1,
                Material.REDSTONE_BLOCK.createBlockData());
        }

        target.getWorld().playSound(targetLoc, Sound.BLOCK_GLASS_BREAK, 1.0f, 1.5f);
        target.getWorld().playSound(targetLoc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.2f, 0.8f);
    }

    private void performExecution() {
        target.damage(Integer.MAX_VALUE, caster);

        playExecutionEffects();
        createExecutionAura();

        MessageUtil.sendMessage(target, "<dark_red>You were executed by " + caster.getScoreboardEntryName() + "!</dark_red>");
        triggerTraits(target, 1);
    }

    private void playExecutionEffects() {
        Location targetLoc = target.getLocation().add(0, 1, 0);

        target.getWorld().spawnParticle(Particle.EXPLOSION, targetLoc, 3, 0.5, 0.5, 0.5, 0);
        target.getWorld().spawnParticle(Particle.BLOCK, targetLoc, 30, 1.0, 1.0, 1.0, 0.2,
            Material.REDSTONE_BLOCK.createBlockData());
        target.getWorld().spawnParticle(Particle.DUST, targetLoc, 25, 0.8, 0.8, 0.8, 0.1,
            new Particle.DustOptions(org.bukkit.Color.MAROON, 2.0f));

        target.getWorld().playSound(targetLoc, Sound.ENTITY_WITHER_DEATH, 0.6f, 1.2f);
        target.getWorld().playSound(targetLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.8f);

        EffectManager effectManager = Spellbook.getInstance().getEffectManager();
        if (effectManager != null) {
            SphereEffect deathBurst = new SphereEffect(effectManager);
            deathBurst.setLocation(targetLoc);
            deathBurst.radius = 3.0f;
            deathBurst.particle = Particle.DUST;
            deathBurst.particles = 50;
            deathBurst.duration = 25;
            deathBurst.start();
        }
    }

    private void createExecutionAura() {
        AoE executionAura = createCircularAoE(target.getLocation(), executionAuraRadius, 1.5, 60)
                .onEnter((aoe, entity) -> {
                    if (!entity.equals(caster) && Spellbook.canAttack(caster, entity)) {
                        entity.getWorld().spawnParticle(Particle.SOUL, entity.getLocation().add(0, 1, 0), 5, 0.3, 0.3, 0.3, 0.05);
                        entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_SOUL_SAND_STEP, 0.4f, 0.7f);
                    }
                });

        executionAura.addDisplay(executionAura.createDisplay()
                .blockDisplay(Material.SOUL_SAND)
                .scale(1.0f)
                .translate(0, 0.1f, 0)
                .build())
                .addDisplay(executionAura.createDisplay()
                .textDisplay("<dark_red><bold>EXECUTION")
                .scale(2.5f)
                .translate(0, 3, 0)
                .build())
                .sendBlockChanges();
    }

    private void performRegularHit() {
        Location targetLoc = target.getLocation().add(0, 1, 0);

        target.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, targetLoc, 8, 0.3, 0.3, 0.3);
        target.getWorld().playSound(targetLoc, Sound.ENTITY_PLAYER_HURT, 0.8f, 1.0f);

        triggerTraits(target, 0);
    }

    @Override
    protected void addSpellPlaceholders() {
        spellAddedPlaceholders.add(Component.text(Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_MAGICAL, bonusPerBleedingMin, bonusPerBleedingMax, "bleedingBonus"), VALUE_COLOR));
        placeholderNames.add("bleedingBonus");
    }
}
