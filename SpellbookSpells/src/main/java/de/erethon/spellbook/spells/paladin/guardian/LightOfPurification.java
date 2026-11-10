package de.erethon.spellbook.spells.paladin.guardian;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellEffect;
import de.erethon.spellbook.combat.BossControl;
import de.erethon.spellbook.combat.CombatSourceContext;
import de.erethon.spellbook.combat.HealingContext;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

/** A brief, visible cleanse pulse with a useful fallback and a small boss interrupt. */
public class LightOfPurification extends GuardianBaseSpell {

    private final double radius = data.getDouble("range", 5);
    private final int effectsToRemove = data.getInt("effectsToRemove", 2);
    private final int devotionPerEffect = data.getInt("devotionPerEffect", 5);
    private final double baseHealing = data.getDouble("baseHealing", 7);
    private final double healingPowerCoefficient = data.getDouble("healingPowerCoefficient", 0.2);
    private final double playerCombatMultiplier = data.getDouble("playerCombatMultiplier", 0.5);
    private final EffectData resistance = Spellbook.getEffectData("Resistance");

    public LightOfPurification(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onPrecast() {
        return super.onPrecast() && hasEnergy(caster, data);
    }

    @Override
    public boolean onCast() {
        if (!super.onCast()) return false;
        Location center = caster.getLocation().clone();
        center.getWorld().playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 0.75f, 1.35f);
        Bukkit.getScheduler().runTaskLater(Spellbook.getInstance().getImplementer(), () -> {
            if (!caster.isValid() || caster.isDead()) return;
            int removed = cleanseAndHeal(caster);
            int allies = 0;
            int enemies = 0;
            for (LivingEntity entity : center.getNearbyLivingEntities(radius)) {
                if (entity == caster || !entity.isValid() || !caster.hasLineOfSight(entity)) continue;
                if (Spellbook.canAttack(caster, entity)) {
                    if (enemies++ < 8) strike(entity);
                } else if (allies++ < 8) {
                    removed += cleanseAndHeal(entity);
                }
            }
            caster.setEnergy(Math.min(caster.getMaxEnergy(), caster.getEnergy()
                    + Math.min(10, removed * devotionPerEffect)));
            center.getWorld().spawnParticle(Particle.ENCHANTED_HIT, center.clone().add(0, 0.5, 0),
                    12, radius * 0.45, 0.25, radius * 0.45, 0.02);
            center.getWorld().playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.9f, 1.5f);
        }, 6L);
        return true;
    }

    private int cleanseAndHeal(LivingEntity ally) {
        List<EffectData> negative = new ArrayList<>();
        for (SpellEffect effect : new ArrayList<>(ally.getEffects())) {
            if (effect.data == null || effect.data.isPositive() || negative.contains(effect.data)) continue;
            negative.add(effect.data);
            if (negative.size() == effectsToRemove) break;
        }
        negative.forEach(ally::removeEffect);
        double healingPower = Math.min(100, Math.max(0,
                caster.getAttribute(Attribute.STAT_HEALINGPOWER).getValue()));
        double heal = baseHealing + healingPower * healingPowerCoefficient;
        if (HealingContext.isPlayerCombat(caster, ally)) heal *= playerCombatMultiplier;
        ally.setHealth(Math.min(ally.getMaxHealth(), ally.getHealth()
                + Math.min(heal, ally.getMaxHealth() * 0.1)));
        if (resistance != null) ally.addEffect(caster, resistance, 40, 1);
        ally.getWorld().spawnParticle(Particle.ENCHANTED_HIT, ally.getEyeLocation(), 8, 0.25, 0.3, 0.25);
        return negative.size();
    }

    private void strike(LivingEntity enemy) {
        double damage = Spellbook.getVariedAttributeBasedDamage(data, caster, enemy, true,
                Attribute.ADVANTAGE_MAGICAL);
        double before = enemy.getHealth() + enemy.getAbsorptionAmount();
        CombatSourceContext.runSpell(this, () -> enemy.damage(damage, caster, PDamageType.MAGIC));
        if (enemy.getHealth() + enemy.getAbsorptionAmount() < before) BossControl.apply(caster, enemy, 30);
    }
}
