package de.erethon.spellbook.spells.assassin.cutthroat;

import de.erethon.spellbook.Spellbook;
import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.combat.CombatSourceContext;
import de.erethon.spellbook.aoe.AoE;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellEffect;
import de.erethon.spellbook.spells.vfx.SkillSignatureVfx;

import de.erethon.spellbook.spells.vfx.TerrainRing;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class CoupDeGrace extends CutthroatBaseSpell {

    // Ultimate ability: Jumps to a target, slashing their throat, dealing massive damage.
    // Damage increased by 10% for each stack of bleeding on the target. Scales with advantage_magical.
    // An earned Wound amplifies a committed lunge. Ordinary low-health mobs take an extra hit.

    private final double bonusPerBleedingMin = data.getDouble("bleedingBonusMin", 0.1);
    private final double bonusPerBleedingMax = data.getDouble("bleedingBonusMax", 0.5);
    private final double executionThreshold = data.getDouble("executionThreshold", 0.25);
    private final double executionAuraRadius = data.getDouble("executionAuraRadius", 4.0);

    private final EffectData bleedingEffectData = Spellbook.getEffectData("Bleeding");

    private boolean hasJumped = false;
    private boolean hasExecuted = false;
    private BukkitRunnable jumpTask = null;

    public CoupDeGrace(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = 100;
    }

    @Override
    protected boolean onPrecast() {
        return super.onPrecast() && lookForTarget(8);
    }

    @Override
    public boolean onCast() {
        if (target == null) {
            return false;
        }
        if (!super.onCast()) {
            return false;
        }
        playAssassinationPrelude();
        return true;
    }

    private void playAssassinationPrelude() {
        Location targetLoc = target.getLocation().add(0, 1, 0);
        SkillSignatureVfx.accent(targetLoc, getClass(), "lead", 4.7f, 25, false);
        SkillSignatureVfx.accent(target.getLocation(), getClass(), "wake", 4.7f, 25, true);
        target.getWorld().playSound(targetLoc, Sound.ENTITY_WITHER_AMBIENT, 0.3f, 2.0f);
    }
    private void performAssassinationJump() {
        hasJumped = true;
        Vector toTarget = target.getLocation().subtract(caster.getLocation()).toVector();
        caster.setVelocity(toTarget.normalize().multiply(1.15).setY(0.35));

        playEnhancedLeapEffects();

        jumpTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!hasExecuted) executeAssassination();
            }
        };
        jumpTask.runTaskLater(Spellbook.getInstance().getImplementer(), 10L);
    }

    private void playEnhancedLeapEffects() {
        Location casterLoc = caster.getLocation().add(0, 1, 0);
        SkillSignatureVfx.accent(casterLoc, getClass(), "wake", 2.9f, 16, false);
        caster.getWorld().playSound(casterLoc, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 1.2f);
        caster.getWorld().playSound(casterLoc, Sound.ENTITY_WITHER_SHOOT, 0.6f, 1.8f);
    }
    @Override
    protected void onTick() {
        if (target == null || target.isDead()) {
            currentTicks = keepAliveTicks;
            onTickFinish();
            return;
        }

        if (!hasJumped && !hasExecuted) {
            performAssassinationJump();
        }
    }

    private void executeAssassination() {
        hasExecuted = true;
        if (target == null || !target.isValid() || target.isDead() || !caster.isValid()
                || caster.getWorld() != target.getWorld()
                || caster.getLocation().distanceSquared(target.getLocation()) > 2.5 * 2.5
                || !caster.hasLineOfSight(target)) {
            caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_PLAYER_ATTACK_NODAMAGE, 0.8f, 0.8f);
            currentTicks = keepAliveTicks;
            onTickFinish();
            return;
        }

        double bonusFromBleeding = 0;
        double scaledBonus = Spellbook.getRangedValue(data, caster, target,
                Attribute.ADVANTAGE_MAGICAL, bonusPerBleedingMin, bonusPerBleedingMax, "bleedingBonus");
        int bleedingStacks = 0;

        for (SpellEffect effect : target.getEffects()) {
            if (effect.data == bleedingEffectData) {
                bleedingStacks += effect.getStacks();
                bonusFromBleeding += scaledBonus * effect.getStacks();
            }
        }

        double baseDamage = Spellbook.getVariedAttributeBasedDamage(data, caster, target, true, Attribute.ADVANTAGE_PHYSICAL);
        double woundMultiplier = target instanceof org.bukkit.entity.Player ? 0.2 : 0.4;
        double totalDamage = baseDamage * (1.0 + Math.min(3, bleedingStacks) * woundMultiplier) + bonusFromBleeding;

        double healthPercent = target.getHealth() / target.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
        if (!(target instanceof org.bukkit.entity.Player) && healthPercent < executionThreshold) {
            totalDamage *= 1.5;
        }
        double before = target.getHealth() + target.getAbsorptionAmount();
        double finalDamage = totalDamage;
        CombatSourceContext.runSpell(this, () -> target.damage(finalDamage, caster, PDamageType.PHYSICAL));
        if (target.getHealth() + target.getAbsorptionAmount() < before) {
            playExecutionStrike(bleedingStacks);
            performRegularHit();
            if (target.isDead()) {
                caster.addEnergy(25);
                playExecutionEffects();
                createExecutionAura();
            }
        }

        currentTicks = keepAliveTicks;
        onTickFinish();
    }

    private void playExecutionStrike(int bleedingStacks) {
        Location targetLoc = target.getLocation().add(0, 1, 0);
        SkillSignatureVfx.accent(targetLoc, getClass(), "main",
                bleedingStacks > 0 ? 4.2f : 3.0f, 22, false);
        target.getWorld().playSound(targetLoc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.2f, 0.8f);
    }

    private void playExecutionEffects() {
        Location targetLoc = target.getLocation().add(0, 1, 0);
        SkillSignatureVfx.accent(targetLoc, getClass(), "main", 6.0f, 30, false);
        SkillSignatureVfx.accent(target.getLocation(), getClass(), "wake", 8.0f, 30, true);
        target.getWorld().playSound(targetLoc, Sound.ENTITY_WITHER_DEATH, 0.6f, 1.2f);
        createExecutionStructure(targetLoc);
    }
    private void createExecutionAura() {
        AoE executionAura = createCircularAoE(target.getLocation(), executionAuraRadius, 1.5, 60)
                .onEnter((aoe, entity) -> {
                    if (!entity.equals(caster) && Spellbook.canAttack(caster, entity)) {
                        entity.getWorld().spawnParticle(Particle.SOUL, entity.getLocation().add(0, 1, 0), 5, 0.3, 0.3, 0.3, 0.05);
                        entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_SOUL_SAND_STEP, 0.4f, 0.7f);
                    }
                });

        TerrainRing terrain = new TerrainRing(target.getLocation(), executionAuraRadius,
                "terrain_band_cutthroat", 48, 0.44f);
        org.bukkit.Bukkit.getScheduler().runTaskLater(Spellbook.getInstance().getImplementer(),
                terrain::close, 60L);
    }
    private void performRegularHit() {
        Location targetLoc = target.getLocation().add(0, 1, 0);

        target.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, targetLoc, 8, 0.3, 0.3, 0.3);
        target.getWorld().playSound(targetLoc, Sound.ENTITY_PLAYER_HURT, 0.8f, 1.0f);

        triggerTraits(target, 0);
    }

    private void createRitualMark(Location casterLoc, Location targetLoc) {
        SkillSignatureVfx.accent(targetLoc, getClass(), "lead", 3.0f, 44, false);
    }

    private void createExecutionStructure(Location center) {
        TerrainRing terrain = new TerrainRing(center, executionAuraRadius,
                "terrain_band_cutthroat", 48, 0.44f);
        org.bukkit.Bukkit.getScheduler().runTaskLater(Spellbook.getInstance().getImplementer(),
                terrain::close, 26L);
    }
    @Override
    protected void cleanup() {
        if (jumpTask != null) jumpTask.cancel();
        super.cleanup();
    }

    @Override
    protected void addSpellPlaceholders() {
        spellAddedPlaceholders.add(Component.text(Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_MAGICAL, bonusPerBleedingMin, bonusPerBleedingMax, "bleedingBonus"), VALUE_COLOR));
        placeholderNames.add("bleedingBonus");
    }
}
