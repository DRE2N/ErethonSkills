package de.erethon.spellbook.spells.warrior.vanguard;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.combat.HealingContext;
import de.erethon.spellbook.spells.vfx.SkillSignatureVfx;
import de.erethon.spellbook.spells.vfx.TerrainRing;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class UnflinchingStance extends VanguardBaseSpell {

    private final int guardTicks = data.getInt("guardTicks", 40);
    private final double baseHealing = data.getDouble("baseHealing", 16);
    private final double healingPowerCoefficient = data.getDouble("healingPowerCoefficient", 0.3);
    private final double baseGuard = data.getDouble("baseGuard", 10);
    private final double resistanceCoefficient = data.getDouble("resistanceCoefficient", 0.18);
    private double remainingGuard;

    private TerrainRing guardRing;

    public UnflinchingStance(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        tickInterval = 1;
        keepAliveTicks = guardTicks;
    }

    @Override
    public boolean onCast() {
        if (!super.onCast()) return false;
        double healingPower = Math.min(100, Math.max(0,
                caster.getAttribute(Attribute.STAT_HEALINGPOWER).getValue()));
        double healing = Math.min(caster.getMaxHealth() * 0.25,
                baseHealing + healingPower * healingPowerCoefficient);
        caster.heal(HealingContext.isPlayerCombat(caster) ? healing * 0.65 : healing);
        double resistance = Math.max(0, caster.getAttribute(Attribute.RESISTANCE_PHYSICAL).getValue())
                + Math.max(0, caster.getAttribute(Attribute.RESISTANCE_MAGICAL).getValue());
        remainingGuard = Math.min(caster.getMaxHealth() * 0.2,
                baseGuard + Math.min(100, resistance) * resistanceCoefficient);

        guardRing = new TerrainRing(caster.getLocation(), 1.15,
                "terrain_band_vanguard", 24, 0.3f);
        SkillSignatureVfx.accent(caster.getLocation(), getClass(), "main", 2.5f, 18, true);
        playStanceEffects();

        return true;
    }

    @Override
    protected void onTick() {
        super.onTick();
        if (guardRing != null && currentTicks % 4 == 0)
            guardRing.move(caster.getLocation());
    }
    @Override
    public double onDamage(LivingEntity attacker, double damage, PDamageType type) {
        double prevented = Math.min(remainingGuard, Math.max(0, damage) * 0.3);
        remainingGuard -= prevented;
        if (prevented > 0) playFacingHit(attacker);
        return super.onDamage(attacker, damage - prevented, type);
    }

    private void playFacingHit(LivingEntity attacker) {
        Vector incoming = attacker == null ? caster.getLocation().getDirection().setY(0)
                : attacker.getLocation().toVector().subtract(caster.getLocation().toVector()).setY(0);
        if (incoming.lengthSquared() < 0.001) incoming = new Vector(0, 0, 1);
        SkillSignatureVfx.accent(caster.getLocation().add(incoming.normalize().multiply(0.85))
                .add(0, 1.1, 0), getClass(), "wake", 1.2f, 8, false);
    }
    public void onCCAbsorbed() {
        playAbsorptionEffect();
    }

    private void playStanceEffects() {
        caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_ANVIL_USE, 0.65f, 1.2f);
    }

    private void playAbsorptionEffect() {
        Location loc = caster.getLocation().add(0, 1, 0);
        SkillSignatureVfx.accent(loc, getClass(), "main", 1.4f, 8, false);
        loc.getWorld().playSound(loc, Sound.BLOCK_ANVIL_PLACE, 0.65f, 1.5f);
    }
    @Override
    protected void onTickFinish() {
        super.onTickFinish();
    }

    @Override
    protected void cleanup() {
        if (guardRing != null) guardRing.close();
        guardRing = null;
        super.cleanup();
    }
}
