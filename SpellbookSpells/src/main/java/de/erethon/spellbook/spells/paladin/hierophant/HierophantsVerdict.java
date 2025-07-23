package de.erethon.spellbook.spells.paladin.hierophant;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.paladin.PaladinBaseSpell;
import de.slikey.effectlib.effect.CircleEffect;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;

public class HierophantsVerdict extends PaladinBaseSpell {

    // The Hierophant slams his weapon down, dealing significant physical and magical damage to nearby enemies.
    // There is a damage falloff based on distance from the caster, which is lessened by the caster's wrath.
    // If your wrath is above 50, the attack also applies weakness to all enemies hit.

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
        CircleEffect circleEffect = new CircleEffect(Spellbook.getInstance().getEffectManager());
        circleEffect.setLocation(caster.getLocation());
        circleEffect.radius = (float) radius;
        circleEffect.particle = Particle.DUST;
        circleEffect.color = Color.GRAY;
        circleEffect.particleCount = 20;
        circleEffect.particleOffsetY = 0.5f;
        circleEffect.duration = 40;
        circleEffect.start();
        for (LivingEntity target : getCaster().getLocation().getNearbyLivingEntities(radius)) {
            if (!Spellbook.canAttack(caster, target)) continue;
            double distance = target.getLocation().distance(caster.getLocation());
            double physDamage = Spellbook.getVariedAttributeBasedDamage(data, caster, target, true, Attribute.ADVANTAGE_PHYSICAL);
            double magicDamage = Spellbook.getVariedAttributeBasedDamage(data, caster, target, true, Attribute.ADVANTAGE_MAGICAL);
            int wrath = caster.getEnergy();
            double damageFalloff = Math.max(0, (damageFalloffPerBlock * distance) - wrath);
            magicDamage -= damageFalloff;
            physDamage -= damageFalloff;
            target.damage(physDamage, caster, PDamageType.PHYSICAL);
            target.damage(magicDamage, caster, PDamageType.MAGIC);
            if (caster.getEnergy() > minWrathForWeakness) {
                target.addEffect(target, weaknessEffect, weaknessDuration, weaknessStacks);
                caster.setEnergy(0);
                caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.RECORDS, 0.8f, 0.8f);
            }
        }
        return super.onCast();
    }
}
