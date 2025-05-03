package de.erethon.spellbook.spells.paladin.hierophant;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.paladin.PaladinBaseSpell;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;

public class WrathfulBurst extends PaladinBaseSpell {

    // The Hierophant unleashes a burst of energy, damaging all enemies in the area.
    // If the Hierophant has more than 50% wrath, the burst deals additional damage and applies a burning effect.

    private final double radius = data.getDouble("radius", 5.0);
    private final int minWrathForBurst = data.getInt("minWrathForBurst", 50);
    private final double burstDamageMultiplier = data.getDouble("burstDamageMultiplier", 1.5f);
    private final int burningMinDuration = data.getInt("burningMinDuration", 60);
    private final int burningMaxDuration = data.getInt("burningMaxDuration", 240);

    public WrathfulBurst(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onPrecast() {
        return super.onPrecast() && hasEnergy(caster, data); // 25
    }

    @Override
    public boolean onCast() {
        World world = caster.getWorld();
        if (caster.getEnergy() < minWrathForBurst) {
            for (LivingEntity livingEntity : caster.getLocation().getNearbyLivingEntities(radius)) {
                if (livingEntity != null && Spellbook.canAttack(caster, livingEntity)) {
                    double damage = Spellbook.getVariedAttributeBasedDamage(data, caster, target, true, Attribute.ADVANTAGE_PHYSICAL);
                    livingEntity.damage(damage, caster, PDamageType.PHYSICAL);
                    world.spawnParticle(Particle.FLAME, livingEntity.getLocation(), 5, 0.5, 0.5, 0.5);
                    world.playSound(livingEntity.getLocation(), org.bukkit.Sound.ENTITY_BLAZE_SHOOT, 1, 0.2f);
                }
            }
            return true;
        } else {
            for (LivingEntity livingEntity : caster.getLocation().getNearbyLivingEntities(radius)) {
                if (livingEntity != null && Spellbook.canAttack(caster, livingEntity)) {
                    double damage = Spellbook.getVariedAttributeBasedDamage(data, caster, target, true, Attribute.ADVANTAGE_PHYSICAL) * burstDamageMultiplier;
                    livingEntity.damage(damage, caster, PDamageType.PHYSICAL);
                    livingEntity.addEffect(livingEntity, Spellbook.getEffectData("Burning"), (int) Spellbook.getRangedValue(data, caster, target, Attribute.ADVANTAGE_MAGICAL, burningMinDuration, burningMaxDuration, "burningDuration"), 1);
                    world.spawnParticle(Particle.FLAME, livingEntity.getLocation(), 8, 0.5, 0.5, 0.5);
                    world.spawnParticle(Particle.SOUL_FIRE_FLAME, livingEntity.getLocation(), 8, 0.5, 0.5, 0.5);
                    world.playSound(livingEntity.getLocation(), org.bukkit.Sound.ENTITY_BLAZE_SHOOT, 1, 2);
                }
            }
        }
        return super.onCast();
    }
}
