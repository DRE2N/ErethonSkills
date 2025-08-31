package de.erethon.spellbook.spells.paladin.hierophant;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.paladin.PaladinBaseSpell;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

public class HierophantsVerdict extends PaladinBaseSpell {

    // The Hierophant slams his weapon down, creating a shockwave that deals damage in a cone in front of him.
    // There is damage falloff based on distance from the caster, which is lessened by the caster's wrath.
    // If your wrath is above 50, the attack also applies weakness and creates cracking ground effects in the impact zone.

    private final double radius = data.getDouble("radius", 4.0);
    private final double damageFalloffPerBlock = data.getDouble("damageFalloffPerBlock", 20);
    private final int weaknessDuration = data.getInt("weaknessDuration", 5) * 20;
    private final int weaknessStacks = data.getInt("weaknessStacks", 1);
    private final int minWrathForWeakness = data.getInt("minWrathForWeakness", 50);

    private final EffectData weaknessEffect = Spellbook.getEffectData("Weakness");

    public HierophantsVerdict(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onPrecast() {
        return super.onPrecast();
    }

    @Override
    public boolean onCast() {
        boolean hasHighWrath = caster.getEnergy() > minWrathForWeakness;
        Vector direction = caster.getEyeLocation().getDirection();

        createConeAoE(caster.getLocation(), radius, 120, 2, direction, 80)
                .onEnter((aoe, entity) -> {
                    if (Spellbook.canAttack(caster, entity)) {
                        double distance = entity.getLocation().distance(caster.getLocation());
                        double physDamage = Spellbook.getVariedAttributeBasedDamage(data, caster, entity, true, Attribute.ADVANTAGE_PHYSICAL);
                        double magicDamage = Spellbook.getVariedAttributeBasedDamage(data, caster, entity, true, Attribute.ADVANTAGE_MAGICAL);

                        int wrath = caster.getEnergy();
                        double damageFalloff = Math.max(0, (damageFalloffPerBlock * distance) - wrath);
                        magicDamage -= damageFalloff;
                        physDamage -= damageFalloff;

                        entity.damage(physDamage, caster, PDamageType.PHYSICAL);
                        entity.damage(magicDamage, caster, PDamageType.MAGIC);

                        entity.getWorld().spawnParticle(Particle.CRIT, entity.getLocation().add(0, 1, 0), 8, 0.5, 0.5, 0.5);
                        entity.getWorld().spawnParticle(Particle.SWEEP_ATTACK, entity.getLocation(), 3, 0.3, 0.3, 0.3);

                        if (hasHighWrath) {
                            entity.addEffect(entity, weaknessEffect, weaknessDuration, weaknessStacks);
                            entity.getWorld().spawnParticle(Particle.SMOKE, entity.getLocation(), 5, 0.3, 0.3, 0.3);
                        }
                    }
                });

        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_RAVAGER_ROAR, 1.5f, 0.8f);
        caster.getWorld().spawnParticle(Particle.EXPLOSION, caster.getLocation().add(0, 0.5, 0), 3, 1.0, 0.5, 1.0);

        if (hasHighWrath) {
            caster.setEnergy(0);
            caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.RECORDS, 0.8f, 0.8f);
        }

        return super.onCast();
    }
}
