package de.erethon.spellbook.spells.assassin.cutthroat;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.assassin.AssassinBaseSpell;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;

public class BloodCraze extends AssassinBaseSpell {

    // The Cutthroat enters a blood frenzy, increasing their movement speed and dealing cleaving damage to nearby enemies.
    // The Cutthroat heals for a portion of the damage dealt. The healing scales with resistance_magical.

    private final double cleavingRange = data.getDouble("cleavingRange", 3.0);
    private final double cleavingDamageMultiplier = data.getDouble("cleavingDamageMultiplier", 0.8);
    private final double damageAsHealingMultiplierMin = data.getDouble("damageAsHealingMultiplierMin", 0.15);
    private final double damageAsHealingMultiplierMax = data.getDouble("damageAsHealingMultiplierMax", 0.25);

    private final AttributeModifier speedBoost = new AttributeModifier(new NamespacedKey("spellbook", "shadow_cloak"), 0.2, AttributeModifier.Operation.ADD_NUMBER);

    private int visualTicks = 20;

    public BloodCraze(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = duration * 20;
    }

    @Override
    public boolean onCast() {
        caster.getAttribute(Attribute.MOVEMENT_SPEED).addTransientModifier(speedBoost);
        return super.onCast();
    }

    @Override
    protected void onTickFinish() {
        super.onTickFinish();
        caster.getAttribute(Attribute.MOVEMENT_SPEED).removeModifier(speedBoost);
    }

    @Override
    protected void onTick() {
        super.onTick();
        visualTicks--;
        if (visualTicks <= 0) {
            visualTicks = 20;
            caster.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, caster.getLocation(), 3, 0.5,0.5,0.5);
        }
    }

    @Override
    public double onAttack(LivingEntity target, double damage, PDamageType type) {
        double cleavingDamage = damage * cleavingDamageMultiplier;
        double healingMultiplier = Spellbook.getRangedValue(data, caster, target, Attribute.RESISTANCE_MAGICAL, damageAsHealingMultiplierMin, damageAsHealingMultiplierMax, "healing");
        double healingAmount = damage * healingMultiplier;
        caster.heal(healingAmount);
        for (LivingEntity entity : target.getLocation().getNearbyLivingEntities(cleavingRange)) {
            if (entity != target && entity != caster && Spellbook.canAttack(caster, target)) {
                entity.damage(cleavingDamage, caster);
                entity.getWorld().spawnParticle(Particle.CRIT, entity.getLocation(), 3, 0.5,0.5,0.5);
            }
        }
        return super.onAttack(target, damage, type);
    }

    @Override
    protected void addSpellPlaceholders() {
        spellAddedPlaceholders.add(Component.text(Spellbook.getRangedValue(data, caster, Attribute.RESISTANCE_MAGICAL, damageAsHealingMultiplierMin, damageAsHealingMultiplierMax, "healing"), VALUE_COLOR));
        placeholderNames.add("healing");
    }
}
