package de.erethon.spellbook.spells.warrior.vanguard;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.combat.BossControl;
import de.erethon.spellbook.combat.CombatSourceContext;
import de.erethon.spellbook.spells.vfx.SkillSignatureVfx;
import de.erethon.spellbook.spells.vfx.SkillVfx;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class Impale extends VanguardBaseSpell {

    private final int range = data.getInt("range", 4);
    private final double healthThreshold = data.getDouble("healthThreshold", 0.4);
    private final int minBleedDuration = data.getInt("minBleedDuration", 4) * 20;
    private final int maxBleedDuration = data.getInt("maxBleedDuration", 8) * 20;
    private final int minBleedStacks = data.getInt("minBleedStacks", 2);
    private final int maxBleedStacks = data.getInt("maxBleedStacks", 4);
    private final int windupTicks = data.getInt("windupTicks", 8);

    private final EffectData bleedEffectData = Spellbook.getEffectData("Bleeding");

    private int ticksElapsed = 0;
    private boolean hasStruck = false;
    private ItemDisplay windupDisplay;
    private Location committedOrigin;
    private Vector committedDirection;
    private Location warnedPoint;

    public Impale(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        tickInterval = 0;
        keepAliveTicks = windupTicks + 5;
    }

    @Override
    protected boolean onPrecast() {
        return super.onPrecast() && lookForTarget(range);
    }

    @Override
    public boolean onCast() {
        if (!super.onCast()) return false;
        committedOrigin = caster.getLocation();
        committedDirection = committedOrigin.getDirection().setY(0);
        if (committedDirection.lengthSquared() < 0.01) committedDirection.setZ(1);
        committedDirection.normalize();
        warnedPoint = target.getLocation();
        spawnWindupDisplay();
        SkillSignatureVfx.accent(warnedPoint, getClass(), "lead", 2.6f,
                windupTicks + 2, true);
        playWindupEffect();
        return true;
    }

    private void spawnWindupDisplay() {
        windupDisplay = trackVfx(SkillVfx.billboard(warnedPoint.clone().add(0, 1.2, 0),
                "sig_warrior_vanguard_impale_wake_f4", 1.8f));
    }

    @Override
    protected void onTick() {
        ticksElapsed++;

        // Track target with windup display
        if (windupDisplay != null && windupDisplay.isValid() && !hasStruck)
            windupDisplay.teleport(warnedPoint.clone().add(0, 1.2, 0));

        if (ticksElapsed == windupTicks && !hasStruck) {
            executeThrust();
            hasStruck = true;
        }
    }

    private void executeThrust() {
        if (target == null || !target.isValid() || target.isDead()
                || target.getWorld() != committedOrigin.getWorld()
                || caster.getLocation().distanceSquared(committedOrigin) > 6.25
                || target.getLocation().distanceSquared(committedOrigin) > range * range
                || !caster.hasLineOfSight(target)) return;
        Vector toward = target.getLocation().toVector().subtract(committedOrigin.toVector()).setY(0);
        if (toward.lengthSquared() < 0.01 || committedDirection.dot(toward.normalize()) < 0.85) return;
        double physicalDamage = Spellbook.getVariedAttributeBasedDamage(data, caster, target, true, Attribute.ADVANTAGE_PHYSICAL);
        double before = target.getHealth() + target.getAbsorptionAmount();
        CombatSourceContext.runSpell(this, () -> target.damage(physicalDamage, caster, PDamageType.PHYSICAL));
        if (target.getHealth() + target.getAbsorptionAmount() >= before) return;
        BossControl.apply(caster, target, 45);

        double currentHealthPercent = target.getHealth() / target.getAttribute(Attribute.MAX_HEALTH).getValue();
        boolean isLowHealth = currentHealthPercent <= healthThreshold;

        int bleedDuration = (int) Spellbook.getRangedValue(data, caster, target, Attribute.ADVANTAGE_PHYSICAL, minBleedDuration, maxBleedDuration, "bleedDuration");
        int bleedStacks = (int) Spellbook.getRangedValue(data, caster, target, Attribute.ADVANTAGE_PHYSICAL, minBleedStacks, maxBleedStacks, "bleedStacks");

        if (isLowHealth) {
            bleedDuration *= 2;
        }

        target.addEffect(caster, bleedEffectData, bleedDuration, bleedStacks);
        playThrustEffects(isLowHealth);
        if (windupDisplay != null && windupDisplay.isValid()) windupDisplay.remove();
        windupDisplay = null;
        SkillSignatureVfx.accent(target.getLocation().add(0, 1, 0), getClass(),
                isLowHealth ? "main" : "wake", isLowHealth ? 2.2f : 1.5f, 10, false);

    }

    private void playWindupEffect() {
        caster.getWorld().spawnParticle(Particle.CRIT, caster.getLocation().add(0, 1.5, 0), 8, 0.5, 0.3, 0.5, 0.1);
        caster.getWorld().playSound(caster.getLocation(), Sound.ITEM_CROSSBOW_LOADING_START, 0.8f, 1.2f);
    }

    private void playThrustEffects(boolean lowHealth) {
        target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0),
                lowHealth ? 8 : 4, 0.25, 0.25, 0.25, 0.1);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT,
                1.0f, lowHealth ? 0.7f : 1.0f);
    }
    @Override
    protected void cleanup() {
        if (windupDisplay != null && windupDisplay.isValid()) {
            windupDisplay.remove();
            windupDisplay = null;
        }
        super.cleanup();
    }

    @Override
    protected void addSpellPlaceholders() {
        double duration = Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_PHYSICAL,
                minBleedDuration, maxBleedDuration, "bleedDuration") / 20.0;
        double stacks = Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_PHYSICAL,
                minBleedStacks, maxBleedStacks, "bleedStacks");
        spellAddedPlaceholders.add(Component.text(Math.round(duration * 10.0) / 10.0, VALUE_COLOR));
        placeholderNames.add("bleedDuration");
        spellAddedPlaceholders.add(Component.text((int) stacks, VALUE_COLOR));
        placeholderNames.add("bleedStacks");
    }
}
