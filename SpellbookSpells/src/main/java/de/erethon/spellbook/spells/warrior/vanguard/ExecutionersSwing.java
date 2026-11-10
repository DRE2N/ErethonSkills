package de.erethon.spellbook.spells.warrior.vanguard;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.combat.BossControl;
import de.erethon.spellbook.combat.CombatSourceContext;
import de.erethon.spellbook.spells.vfx.SkillSignatureVfx;
import de.erethon.spellbook.spells.vfx.SkillVfx;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.Location;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class ExecutionersSwing extends VanguardBaseSpell {

    private static final Color HIGH_MOMENTUM_COLOR = Color.fromRGB(200, 40, 40);

    private final int range = data.getInt("range", 4);
    private final int channelDuration = data.getInt("channelTicks", 16);
    private final double missingHealthMultiplier = data.getDouble("missingHealthMultiplier", 2.0);
    private final double momentumDamagePerStack = data.getDouble("momentumDamagePerStack", 0.15);
    private final int momentumPerStack = data.getInt("momentumPerStack", 10);

    private int ticksElapsed = 0;
    private boolean hasExecuted = false;
    private int momentumConsumed = 0;

    private ItemDisplay channelDisplay;


    public ExecutionersSwing(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        tickInterval = 0;
        keepAliveTicks = channelDuration + 10;
    }

    @Override
    protected boolean onPrecast() {
        return super.onPrecast() && lookForTarget(range);
    }

    @Override
    public boolean onCast() {
        momentumConsumed = caster.getEnergy() / momentumPerStack;
        caster.setEnergy(0);

        spawnChannelDisplay();
        playChannelStartEffect();
        return super.onCast();
    }

    private void spawnChannelDisplay() {
        channelDisplay = trackVfx(SkillVfx.billboard(target.getLocation().add(0, 2.25, 0),
                "sig_warrior_vanguard_executionersswing_lead_f4", 1.6f));
    }
    @Override
    protected void onTick() {
        ticksElapsed++;

        if (ticksElapsed <= channelDuration) {
            updateChannelDisplay();
            playChannelEffect();
        } else if (ticksElapsed == channelDuration + 1 && !hasExecuted) {
            executeSwing();
            hasExecuted = true;
        }
    }

    private void updateChannelDisplay() {
        if (channelDisplay != null && channelDisplay.isValid() && target != null && target.isValid())
            channelDisplay.teleport(target.getLocation().add(0, 2.25, 0));
    }

    private void playChannelStartEffect() {
        caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_ANVIL_USE, 0.7f, 0.65f);
    }

    private void playChannelEffect() {
        if (ticksElapsed % 5 == 0) caster.getWorld().playSound(caster.getLocation(),
                Sound.BLOCK_GRINDSTONE_USE, 0.25f,
                0.8f + 0.6f * ticksElapsed / channelDuration);
    }
    private void executeSwing() {
        if (target == null || !target.isValid() || target.isDead()
                || caster.getWorld() != target.getWorld()
                || caster.getLocation().distanceSquared(target.getLocation()) > range * range
                || !caster.hasLineOfSight(target) || !Spellbook.canAttack(caster, target)) return;
        if (channelDisplay != null && channelDisplay.isValid()) channelDisplay.remove();
        channelDisplay = null;
        SkillSignatureVfx.accent(target.getLocation().add(0, 1, 0), getClass(),
                "main", 3.0f, 12, false);
        double baseDamage = Spellbook.getVariedAttributeBasedDamage(data, caster, target, true, Attribute.ADVANTAGE_PHYSICAL);
        double missingHealthPercent = 1.0 - (target.getHealth() / target.getAttribute(Attribute.MAX_HEALTH).getValue());
        double missingHealthBonus = baseDamage * missingHealthMultiplier * missingHealthPercent;
        double momentumBonus = baseDamage * momentumDamagePerStack * momentumConsumed;
        double totalDamage = Math.min(baseDamage * (target instanceof Player ? 1.8 : 3.0),
                baseDamage + missingHealthBonus + momentumBonus);

        double before = target.getHealth() + target.getAbsorptionAmount();
        CombatSourceContext.runSpell(this, () -> target.damage(totalDamage, caster, PDamageType.PHYSICAL));
        if (target.getHealth() + target.getAbsorptionAmount() >= before) return;
        BossControl.apply(caster, target, 35);
        playExecutionSeal();
        playExecutionEffects(missingHealthPercent > 0.5, momentumConsumed > 5);
    }

    private void playExecutionSeal() {
        SkillSignatureVfx.accent(target.getLocation(), getClass(), "main", 3.6f, 14, true);
        SkillSignatureVfx.accent(target.getLocation().add(0, 1.1, 0), getClass(),
                "wake", 2.0f, 10, false);
    }

    private void playExecutionEffects(boolean highMissingHealth, boolean highMomentum) {
        target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0),
                highMissingHealth ? 14 : 9, 0.4, 0.4, 0.4, 0.2);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT,
                1.0f, highMissingHealth ? 0.65f : 0.85f);
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_ANVIL_LAND,
                highMomentum ? 1.0f : 0.7f, 0.8f);
    }
    @Override
    protected void cleanup() {
        if (channelDisplay != null && channelDisplay.isValid()) {
            channelDisplay.remove();
            channelDisplay = null;
        }

        super.cleanup();
    }
}
