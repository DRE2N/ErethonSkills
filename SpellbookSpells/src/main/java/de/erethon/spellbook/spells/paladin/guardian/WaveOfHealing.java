package de.erethon.spellbook.spells.paladin.guardian;
import de.erethon.spellbook.utils.TargetedParticles;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.combat.HealingContext;
import net.kyori.adventure.text.Component;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Display;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WaveOfHealing extends GuardianBaseSpell {

    // A dependable self rescue that may also save one deliberately aimed ally.
    private final int range = data.getInt("range", 8);
    private final double baseHealing = data.getDouble("baseHealing", 18);
    private final double healingPowerCoefficient = data.getDouble("healingPowerCoefficient", 0.35);
    private final double healingPerDevotion = data.getDouble("healingPerDevotion", 0.15);
    private final int maximumDevotionSpend = data.getInt("maximumDevotionSpend", 40);
    private final double playerCombatMultiplier = data.getDouble("playerCombatMultiplier", 0.65);
    private final double maximumHealthFraction = data.getDouble("maximumHealthFraction", 0.3);
    private final int coneAngle = data.getInt("coneAngle", 60);

    public WaveOfHealing(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onPrecast() {
        return super.onPrecast() && hasEnergy(caster, data);
    }

    @Override
    public boolean onCast() {
        int devotionSpent = Math.min(maximumDevotionSpend, Math.max(0, (int) caster.getEnergy() - energyCost));
        if (!super.onCast()) return false;
        caster.setEnergy(Math.max(0, caster.getEnergy() - devotionSpent));
        double healingPower = Math.min(100, Math.max(0, caster.getAttribute(Attribute.STAT_HEALINGPOWER).getValue()));
        double totalHealing = Math.max(0, baseHealing + healingPowerCoefficient * healingPower
                + devotionSpent * healingPerDevotion);

        Set<LivingEntity> healedEntities = new HashSet<>();
        healTarget(caster, healedEntities, totalHealing);
        if (caster.getTargetEntity(range, true) instanceof LivingEntity ally
                && ally != caster && !Spellbook.canAttack(caster, ally) && caster.hasLineOfSight(ally)) {
            healTarget(ally, healedEntities, totalHealing);
        }

        caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.RECORDS, 1.0f, 0.8f);

        triggerTraits(healedEntities);
        return true;
    }

    private void healTarget(LivingEntity target, Set<LivingEntity> healedEntities, double totalHealing) {
        healedEntities.add(target);

        double maxHealth = target.getAttribute(Attribute.MAX_HEALTH).getValue();
        double contextHealing = HealingContext.isPlayerCombat(caster, target)
                ? totalHealing * playerCombatMultiplier : totalHealing;
        target.setHealth(Math.clamp(target.getHealth() + Math.min(contextHealing,
                maxHealth * maximumHealthFraction), 0, maxHealth));

        TargetedParticles.spawnForOthers(Particle.HEART, target,
                target.getLocation().add(0, 2, 0),
                2, 0.4, 0.4, 0.4);
        TargetedParticles.spawnForOthers(Particle.TOTEM_OF_UNDYING, target,
                target.getLocation().add(0, 1, 0),
                3, 0.25, 0.6, 0.25, 0.02);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.RECORDS, 0.3f, 1.5f);
    }

    @Override
    protected void addSpellPlaceholders() {
        spellAddedPlaceholders.add(Component.text(baseHealing + healingPowerCoefficient * Math.min(100,
                Math.max(0, caster.getAttribute(Attribute.STAT_HEALINGPOWER).getValue())), ATTR_HEALING_POWER_COLOR));
        placeholderNames.add("baseHeal");
    }
}
